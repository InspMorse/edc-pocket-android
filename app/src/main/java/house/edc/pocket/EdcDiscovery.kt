package house.edc.pocket

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class EdcDiscovery(context: Context) {
    private val nsd = context.applicationContext.getSystemService(NsdManager::class.java)

    suspend fun discover(timeoutMs: Long = 4_000L): List<DiscoveredHost> {
        val fromNsd = discoverNsd(timeoutMs)
        if (fromNsd.isNotEmpty()) return fromNsd.distinctBy { it.baseUrl }
        return emptyList()
    }

    private suspend fun discoverNsd(timeoutMs: Long): List<DiscoveredHost> =
        suspendCancellableCoroutine { cont ->
            val found = linkedMapOf<String, DiscoveredHost>()
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) = Unit
                override fun onServiceResolved(resolved: NsdServiceInfo) {
                    val host = resolved.host?.hostAddress ?: return
                    val port = if (resolved.port > 0) resolved.port else 8765
                    val baseUrl = "http://$host:$port"
                    found[baseUrl] = DiscoveredHost(
                        name = resolved.serviceName.ifBlank { "EDC" },
                        baseUrl = baseUrl,
                        source = "mDNS",
                    )
                }
            }
            val discoveryListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) = Unit
                override fun onServiceFound(service: NsdServiceInfo) {
                    nsd.resolveService(service, resolveListener)
                }
                override fun onServiceLost(service: NsdServiceInfo) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    if (cont.isActive) cont.resume(emptyList())
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            }
            runCatching {
                nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            }.onFailure {
                if (cont.isActive) cont.resume(emptyList())
                return@suspendCancellableCoroutine
            }
            cont.invokeOnCancellation {
                runCatching { nsd.stopServiceDiscovery(discoveryListener) }
            }
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                runCatching { nsd.stopServiceDiscovery(discoveryListener) }
                if (cont.isActive) cont.resume(found.values.toList())
            }, timeoutMs)
        }

    companion object {
        const val SERVICE_TYPE = "_edc._tcp."
    }
}
