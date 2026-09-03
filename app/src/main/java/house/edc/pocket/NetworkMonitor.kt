package house.edc.pocket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class NetworkKind {
    NONE,
    WIFI,
    CELLULAR,
    VPN,
    OTHER,
}

class NetworkMonitor(context: Context) {
    private val connectivity =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    val networkKind: Flow<NetworkKind> = callbackFlow {
        fun emitCurrent() {
            trySend(classify(connectivity))
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = emitCurrent()
            override fun onLost(network: Network) = emitCurrent()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) = emitCurrent()
        }
        val request = NetworkRequest.Builder().build()
        connectivity.registerNetworkCallback(request, callback)
        emitCurrent()
        awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

internal fun classify(manager: ConnectivityManager): NetworkKind {
    val network = manager.activeNetwork ?: return NetworkKind.NONE
    val caps = manager.getNetworkCapabilities(network) ?: return NetworkKind.NONE
    if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
        return NetworkKind.NONE
    }
    return when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkKind.VPN
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.WIFI
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkKind.CELLULAR
        else -> NetworkKind.OTHER
    }
}
