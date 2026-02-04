package com.miragekit.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.miragekit.protocol.DeviceType
import com.miragekit.protocol.MirageHost
import com.miragekit.protocol.MirageHostCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.nio.charset.StandardCharsets
import java.util.UUID

class MirageDiscovery(context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val serviceType = "_mirage._tcp"

    fun discoverHosts(): Flow<List<MirageHost>> = callbackFlow {
        val hosts = mutableMapOf<String, MirageHost>()

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                // Ensure it matches our service type
                if (serviceInfo.serviceType.contains("mirage")) {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // Retry or ignore
                        }

                        override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                            val host = parseServiceInfo(resolvedService)
                            hosts[resolvedService.serviceName] = host
                            trySend(hosts.values.toList())
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                hosts.remove(serviceInfo.serviceName)
                trySend(hosts.values.toList())
            }

            override fun onDiscoveryStopped(serviceType: String) {
                close()
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        awaitClose {
            try {
                nsdManager.stopDiscovery(discoveryListener)
            } catch (e: Exception) {
                // Ignore if already stopped
            }
        }
    }

    private fun parseServiceInfo(info: NsdServiceInfo): MirageHost {
        val txtRecord = info.attributes.mapValues { String(it.value, StandardCharsets.UTF_8) }
        val capabilities = MirageHostCapabilities.fromTxtRecord(txtRecord)

        val id = capabilities.deviceID ?: UUID.randomUUID().toString()

        return MirageHost(
            id = id,
            name = info.serviceName,
            deviceType = DeviceType.mac, // Assume Mac for now as it's the only host supported by MirageKit
            endpoint = info.host.hostAddress ?: "",
            port = info.port,
            capabilities = capabilities
        )
    }
}
