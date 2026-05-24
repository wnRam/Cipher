package uz.angrykitten.spygame.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uz.angrykitten.spygame.model.Message
import uz.angrykitten.spygame.model.Player
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeerSocketManager @Inject constructor() {

    companion object {
        private const val TAG = "PeerSocketManager"
        private const val CONNECT_TIMEOUT_MS = 5000
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private var scope: CoroutineScope = newScope()
    private var listenJob: Job? = null

    private fun newScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    suspend fun connect(host: String, port: Int, player: Player): Boolean {
        if (!scope.isActive) scope = newScope()
        _connectionState.value = ConnectionState.CONNECTING
        return withContext(Dispatchers.IO) {
            try {
                val newSocket = Socket()
                newSocket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                socket = newSocket
                // Buffered writer with autoFlush=true keeps protocol semantics
                // (one message per println, flushed immediately) while avoiding
                // unbuffered byte-by-byte syscalls.
                writer = PrintWriter(
                    BufferedWriter(OutputStreamWriter(newSocket.getOutputStream())),
                    /* autoFlush = */ true
                )
                reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))

                // Send join message
                val joinMessage = Message.PlayerJoined(player)
                writer?.println(json.encodeToString<Message>(joinMessage))

                _connectionState.value = ConnectionState.CONNECTED
                startListening()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                _connectionState.value = ConnectionState.ERROR
                false
            }
        }
    }

    private fun startListening() {
        listenJob = scope.launch {
            try {
                while (isActive) {
                    val line = reader?.readLine() ?: break
                    try {
                        val message = json.decodeFromString<Message>(line)
                        if (message is Message.Ping) {
                            // Reply to keep the host's heartbeat tracker happy;
                            // never surface Ping to game-logic listeners.
                            try {
                                writer?.println(json.encodeToString<Message>(Message.Pong))
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to send Pong", e)
                            }
                            continue
                        }
                        _incomingMessages.emit(message)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Disconnected from host: ${e.message}")
            } finally {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    fun send(message: Message) {
        scope.launch {
            try {
                val encoded = json.encodeToString(message)
                writer?.println(encoded)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            }
        }
    }

    fun disconnect() {
        listenJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        writer = null
        reader = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }
}
