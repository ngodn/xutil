package id.abtech.xlinxutil.xray.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.*
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import android.util.Log
import androidx.annotation.RequiresApi
import id.abtech.xlinxutil.R
import id.abtech.xlinxutil.xray.extension.defaultDPreference
//import id.abtech.xlinxutil.ui.PerAppProxyActivity
//import id.abtech.xlinxutil.ui.SettingsActivity
import id.abtech.xlinxutil.xray.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.SoftReference

class V2RayVpnService : VpnService(), ServiceControl {
    companion object {
        //        const val PREF_BYPASS_MAINLAND = "pref_bypass_mainland"
        //        const val PREF_START_ON_BOOT = "pref_start_on_boot"
        const val PREF_PER_APP_PROXY = "pref_per_app_proxy"
        //        const val PREF_MUX_ENAimport libv2ray.Libv2rayBLED = "pref_mux_enabled"
        const val PREF_SPEED_ENABLED = "pref_speed_enabled"
        const val PREF_SNIFFING_ENABLED = "pref_sniffing_enabled"
        const val PREF_PROXY_SHARING = "pref_proxy_sharing_enabled"
        const val PREF_LOCAL_DNS_ENABLED = "pref_local_dns_enabled"
        const val PREF_REMOTE_DNS = "pref_remote_dns"
        const val PREF_DOMESTIC_DNS = "pref_domestic_dns"

//        const val PREF_SOCKS_PORT = "pref_socks_port"
//        const val PREF_HTTP_PORT = "pref_http_port"

        const val PREF_ROUTING_DOMAIN_STRATEGY = "pref_routing_domain_strategy"
        const val PREF_ROUTING_MODE = "pref_routing_mode"
        const val PREF_ROUTING_CUSTOM = "pref_routing_custom"
        //        const val PREF_DONATE = "pref_donate"
        //        const val PREF_LICENSES = "pref_licenses"
//        const val PREF_FEEDBACK = "pref_feedback"
//        const val PREF_TG_GROUP = "pref_tg_group"
        //        const val PREF_AUTO_RESTART = "pref_auto_restart"
        const val PREF_FORWARD_IPV6 = "pref_forward_ipv6"

        const val PREF_PER_APP_PROXY_SET = "pref_per_app_proxy_set"
        const val PREF_BYPASS_APPS = "pref_bypass_apps"
    }

    private lateinit var mInterface: ParcelFileDescriptor

    /**
        * Unfortunately registerDefaultNetworkCallback is going to return our VPN interface: https://android.googlesource.com/platform/frameworks/base/+/dda156ab0c5d66ad82bdcf76cda07cbc0a9c8a2e
        *
        * This makes doing a requestNetwork with REQUEST necessary so that we don't get ALL possible networks that
        * satisfies default network capabilities but only THE default network. Unfortunately we need to have
        * android.permission.CHANGE_NETWORK_STATE to be able to call requestNetwork.
        *
        * Source: https://android.googlesource.com/platform/frameworks/base/+/2df4c7d/services/core/java/com/android/server/ConnectivityService.java#887
        */
    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkRequest by lazy {
        NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .build()
    }

    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // it's a good idea to refresh capabilities
                setUnderlyingNetworks(arrayOf(network))
            }
            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        V2RayServiceManager.serviceControl = SoftReference(this)
    }

    override fun onRevoke() {
        stopV2Ray()
    }

    override fun onLowMemory() {
        stopV2Ray()
        super.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopV2Ray()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setup(parameters: String) {

        val prepare = prepare(this)
        if (prepare != null) {
            return
        }

        // If the old interface has exactly the same parameters, use it!
        // Configure a builder while parsing the parameters.
        val builder = Builder()
        val enableLocalDns = defaultDPreference.getPrefBoolean(PREF_LOCAL_DNS_ENABLED, false)
        val routingMode = defaultDPreference.getPrefString(PREF_ROUTING_MODE, "0")

        parameters.split(" ")
                .map { it.split(",") }
                .forEach {
                    when (it[0][0]) {
                        'm' -> builder.setMtu(java.lang.Short.parseShort(it[1]).toInt())
                        's' -> builder.addSearchDomain(it[1])
                        'a' -> builder.addAddress(it[1], Integer.parseInt(it[2]))
                        'r' -> {
                            if (routingMode == "1" || routingMode == "3") {
                                if (it[1] == "::") { //not very elegant, should move Vpn setting in Kotlin, simplify go code
                                    builder.addRoute("2000::", 3)
                                } else {
                                    resources.getStringArray(R.array.bypass_private_ip_address).forEach { cidr ->
                                        val addr = cidr.split('/')
                                        builder.addRoute(addr[0], addr[1].toInt())
                                    }
                                }
                            } else {
                                builder.addRoute(it[1], Integer.parseInt(it[2]))
                            }
                        }
                        'd' -> builder.addDnsServer(it[1])
                    }
                }

        if(!enableLocalDns) {
            Utils.getRemoteDnsServers(defaultDPreference)
                .forEach {
                    builder.addDnsServer(it)
                }
        }

        builder.setSession(V2RayServiceManager.currentConfigName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                defaultDPreference.getPrefBoolean(PREF_PER_APP_PROXY, false)) {
            val apps = defaultDPreference.getPrefStringSet(PREF_PER_APP_PROXY_SET, null)
            val bypassApps = defaultDPreference.getPrefBoolean(PREF_BYPASS_APPS, false)
            apps?.forEach {
                try {
                    if (bypassApps)
                        builder.addDisallowedApplication(it)
                    else
                        builder.addAllowedApplication(it)
                } catch (e: PackageManager.NameNotFoundException) {
                    //Logger.d(e)
                }
            }
        }

        // Close the old interface since the parameters have been changed.
        try {
            mInterface.close()
        } catch (ignored: Exception) {
            // ignored
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
//            builder.setMetered(false)
//        }

        // Create a new interface using the builder and save the parameters.
        try {
            mInterface = builder.establish()!!
        } catch (e: Exception) {
            // non-nullable lateinit var
            e.printStackTrace()
            stopV2Ray()
        }

        sendFd()
    }

    private fun sendFd() {
        val fd = mInterface.fileDescriptor
        val path = File(Utils.packagePath(applicationContext), "sock_path").absolutePath

        GlobalScope.launch(Dispatchers.IO) {
            var tries = 0
            while (true) try {
                Thread.sleep(1000L shl tries)
                Log.d(packageName, "sendFd tries: $tries")
                LocalSocket().use { localSocket ->
                    localSocket.connect(LocalSocketAddress(path, LocalSocketAddress.Namespace.FILESYSTEM))
                    localSocket.setFileDescriptorsForSend(arrayOf(fd))
                    localSocket.outputStream.write(42)
                }
                break
            } catch (e: Exception) {
                Log.d(packageName, e.toString())
                if (tries > 5) break
                tries += 1
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        V2RayServiceManager.startV2rayPoint()
        return START_STICKY
        //return super.onStartCommand(intent, flags, startId)
    }

    private fun stopV2Ray(isForced: Boolean = true) {
//        val configName = defaultDPreference.getPrefString(PREF_CURR_CONFIG_GUID, "")
//        val emptyInfo = VpnNetworkInfo()
//        val info = loadVpnNetworkInfo(configName, emptyInfo)!! + (lastNetworkInfo ?: emptyInfo)
//        saveVpnNetworkInfo(configName, info)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectivity.unregisterNetworkCallback(defaultNetworkCallback)
            } catch (ignored: Exception) {
                // ignored
            }
        }

        V2RayServiceManager.stopV2rayPoint()

        if (isForced) {
            //stopSelf has to be called ahead of mInterface.close(). otherwise v2ray core cannot be stooped
            //It's strage but true.
            //This can be verified by putting stopself() behind and call stopLoop and startLoop
            //in a row for several times. You will find that later created v2ray core report port in use
            //which means the first v2ray core somehow failed to stop and release the port.
            stopSelf()

            try {
                mInterface.close()
            } catch (ignored: Exception) {
                // ignored
            }

        }
    }

    override fun getService(): Service {
        return this
    }

    override fun startService(parameters: String) {
        setup(parameters)
    }

    override fun stopService() {
        stopV2Ray(true)
    }

    override fun vpnProtect(socket: Int): Boolean {
        return protect(socket)
    }

}
