package id.abtech.xlinxutil.xray.service

import android.app.Service

interface ServiceControl {
    fun getService(): Service

    fun startService(parameters: String)

    fun stopService()

    fun vpnProtect(socket: Int): Boolean

}
