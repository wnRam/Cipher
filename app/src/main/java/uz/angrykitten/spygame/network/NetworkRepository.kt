package uz.angrykitten.spygame.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import uz.angrykitten.spygame.model.Message
import uz.angrykitten.spygame.model.Player
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    val nsdHelper: NsdHelper,
    val hostSocketManager: HostSocketManager,
    val peerSocketManager: PeerSocketManager
) {
    companion object {
        private const val TAG = "NetworkRepository"
    }

    fun isWifiConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Observe WiFi connectivity changes as a cold flow. The callback is
     * automatically unregistered when the collector cancels (e.g. ViewModel
     * cleared, app backgrounded with WhileSubscribed sharing).
     */
    fun wifiConnectedFlow(): Flow<Boolean> = callbackFlow {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        // Emit current state immediately so subscribers don't have to wait
        // for the first connectivity callback.
        trySend(isWifiConnected())
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(isWifiConnected()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                trySend(caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            }
        }
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "registerNetworkCallback failed", e)
        }
        awaitClose {
            try {
                cm.unregisterNetworkCallback(callback)
            } catch (_: Exception) { /* already unregistered */ }
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local IP", e)
        }
        return null
    }

    fun generateRoomCode(ip: String, port: Int): String {
        val hash = (ip + port.toString()).hashCode()
        val code = Math.abs(hash % 1000000)
        return code.toString().padStart(6, '0')
    }

    // Host functions
    fun startHosting(roomCode: String): Int {
        val port = hostSocketManager.startServer()
        nsdHelper.registerService(port, roomCode)
        return port
    }

    /** Open the ServerSocket without registering NSD yet — caller derives the
     *  room code from the bound port, then calls [registerNsd]. */
    fun startServer(): Int = hostSocketManager.startServer()

    fun registerNsd(port: Int, roomCode: String) {
        nsdHelper.registerService(port, roomCode)
    }

    fun broadcastMessage(message: Message) {
        hostSocketManager.broadcast(message)
    }

    fun sendToPlayer(playerId: String, message: Message) {
        hostSocketManager.sendTo(playerId, message)
    }

    fun kickPlayer(playerId: String) {
        hostSocketManager.disconnectClient(playerId)
    }

    val hostIncomingMessages: SharedFlow<Pair<String, Message>>
        get() = hostSocketManager.incomingMessages

    val hostClientConnected: SharedFlow<String>
        get() = hostSocketManager.clientConnected

    val hostClientDisconnected: SharedFlow<String>
        get() = hostSocketManager.clientDisconnected

    // Peer functions
    suspend fun joinRoom(host: String, port: Int, player: Player): Boolean {
        return peerSocketManager.connect(host, port, player)
    }

    fun sendToPeer(message: Message) {
        peerSocketManager.send(message)
    }

    val peerIncomingMessages: SharedFlow<Message>
        get() = peerSocketManager.incomingMessages

    val peerConnectionState: StateFlow<PeerSocketManager.ConnectionState>
        get() = peerSocketManager.connectionState

    // Cleanup
    fun stopHosting() {
        nsdHelper.cleanup()
        hostSocketManager.shutdown()
    }

    fun disconnectPeer() {
        peerSocketManager.disconnect()
        nsdHelper.stopDiscovery()
    }

    fun cleanup() {
        stopHosting()
        disconnectPeer()
    }
}
