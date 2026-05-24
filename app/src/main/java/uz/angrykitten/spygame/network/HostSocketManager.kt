package uz.angrykitten.spygame.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uz.angrykitten.spygame.model.Message
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HostSocketManager @Inject constructor() {

    companion object {
        private const val TAG = "HostSocketManager"
        private const val HEARTBEAT_INTERVAL_MS = 5_000L
        private const val HEARTBEAT_TIMEOUT_MS = 15_000L
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
    }

    private var serverSocket: ServerSocket? = null
    private val connectedClients = mutableMapOf<String, ClientConnection>()
    // Re-created on each startServer() because shutdown() cancels the scope —
    // a cancelled scope can't be reused, which would silently break re-hosting.
    private var scope: CoroutineScope = newScope()

    private fun newScope() = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _incomingMessages = MutableSharedFlow<Pair<String, Message>>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Pair<String, Message>> = _incomingMessages.asSharedFlow()

    private val _clientConnected = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val clientConnected: SharedFlow<String> = _clientConnected.asSharedFlow()

    private val _clientDisconnected = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val clientDisconnected: SharedFlow<String> = _clientDisconnected.asSharedFlow()

    val port: Int get() = serverSocket?.localPort ?: 0

    data class ClientConnection(
        val socket: Socket,
        val writer: PrintWriter,
        val reader: BufferedReader,
        var job: Job? = null,
        @Volatile var lastSeenMs: Long = System.currentTimeMillis()
    )

    private var heartbeatJob: Job? = null

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                val now = System.currentTimeMillis()
                val encoded = json.encodeToString<Message>(Message.Ping)
                // Snapshot to avoid concurrent modification.
                connectedClients.toMap().forEach { (id, conn) ->
                    if (now - conn.lastSeenMs > HEARTBEAT_TIMEOUT_MS) {
                        Log.w(TAG, "Client $id timed out (no pong)")
                        scope.launch { removeClient(id) }
                        return@forEach
                    }
                    try {
                        conn.writer.println(encoded)
                    } catch (e: Exception) {
                        Log.e(TAG, "Heartbeat send failed for $id", e)
                        scope.launch { removeClient(id) }
                    }
                }
            }
        }
    }

    fun startServer(): Int {
        if (!scope.isActive) scope = newScope()
        serverSocket = ServerSocket(0)
        val port = serverSocket!!.localPort
        Log.d(TAG, "Server started on port $port")

        scope.launch {
            while (isActive) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    handleNewClient(clientSocket)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error accepting client", e)
                    }
                    break
                }
            }
        }

        startHeartbeat()

        return port
    }

    private fun handleNewClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                // BufferedWriter cuts per-message syscalls vs raw OutputStream.
                // autoFlush=true on PrintWriter keeps the existing semantics:
                // each println() lands on the wire immediately so peers see
                // messages without us tracking flushes manually.
                val writer = PrintWriter(
                    BufferedWriter(OutputStreamWriter(socket.getOutputStream())),
                    /* autoFlush = */ true
                )

                // First message should be PlayerJoined with player info
                val firstLine = reader.readLine() ?: return@launch
                val firstMessage = json.decodeFromString<Message>(firstLine)

                if (firstMessage is Message.PlayerJoined) {
                    val playerId = firstMessage.player.id
                    val connection = ClientConnection(socket, writer, reader)

                    connectedClients[playerId] = connection
                    _clientConnected.emit(playerId)
                    _incomingMessages.emit(playerId to firstMessage)

                    // Start listening for messages from this client
                    connection.job = scope.launch {
                        listenToClient(playerId, connection)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling new client", e)
                socket.close()
            }
        }
    }

    private suspend fun listenToClient(playerId: String, connection: ClientConnection) {
        try {
            while (currentCoroutineContext().isActive) {
                val line = connection.reader.readLine() ?: break
                connection.lastSeenMs = System.currentTimeMillis()
                try {
                    val message = json.decodeFromString<Message>(line)
                    // Pongs are handled by the heartbeat tracker; don't fan them
                    // out to game-logic listeners.
                    if (message is Message.Pong) continue
                    _incomingMessages.emit(playerId to message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing message from $playerId", e)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client $playerId disconnected: ${e.message}")
        } finally {
            removeClient(playerId)
        }
    }

    fun broadcast(message: Message) {
        // JSON encode + socket write off the caller's thread (often Main).
        // Snapshot the client list to avoid CME if disconnects fire mid-write.
        scope.launch {
            val encoded = json.encodeToString(message)
            connectedClients.toMap().forEach { (id, connection) ->
                try {
                    connection.writer.println(encoded)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending to $id", e)
                    scope.launch { removeClient(id) }
                }
            }
        }
    }

    fun sendTo(playerId: String, message: Message) {
        scope.launch {
            val encoded = json.encodeToString(message)
            try {
                connectedClients[playerId]?.writer?.println(encoded)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to $playerId", e)
                scope.launch { removeClient(playerId) }
            }
        }
    }

    private suspend fun removeClient(playerId: String) {
        connectedClients[playerId]?.let { connection ->
            connection.job?.cancel()
            try {
                connection.socket.close()
            } catch (_: Exception) {}
        }
        connectedClients.remove(playerId)
        _clientDisconnected.emit(playerId)
    }

    fun disconnectClient(playerId: String) {
        scope.launch { removeClient(playerId) }
    }

    fun getConnectedPlayerIds(): Set<String> = connectedClients.keys.toSet()

    fun shutdown() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        scope.cancel()
        connectedClients.values.forEach { connection ->
            try {
                connection.socket.close()
            } catch (_: Exception) {}
        }
        connectedClients.clear()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
