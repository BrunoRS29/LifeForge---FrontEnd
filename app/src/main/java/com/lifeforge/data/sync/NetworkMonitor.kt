package com.lifeforge.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/** Conectividade do aparelho, para decidir se vale tentar a rede agora. */
interface NetworkMonitor {
    /** Emite o estado atual e a cada mudança de rede. */
    val isOnline: Flow<Boolean>

    /** Leitura instantânea (sem suspender) do estado atual. */
    fun isOnlineNow(): Boolean
}

/**
 * [NetworkMonitor] baseado no `ConnectivityManager`: considera online a rede
 * padrão com capacidade de acesso à internet.
 */
@Singleton
class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext context: Context,
) : NetworkMonitor {

    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override fun isOnlineNow(): Boolean {
        val network = connectivity?.activeNetwork ?: return false
        return connectivity.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isOnlineNow())
            }

            override fun onLost(network: Network) {
                trySend(isOnlineNow())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(isOnlineNow())
            }
        }
        trySend(isOnlineNow())
        connectivity?.registerDefaultNetworkCallback(callback)
        awaitClose { connectivity?.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged().conflate()
}
