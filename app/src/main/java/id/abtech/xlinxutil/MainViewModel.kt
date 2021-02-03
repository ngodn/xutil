package id.abtech.xlinxutil

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import id.abtech.xlinxutil.AngApplication
import id.abtech.xlinxutil.AppConfig
import id.abtech.xlinxutil.R
import id.abtech.xlinxutil.xray.dto.EConfigType
import id.abtech.xlinxutil.xray.extension.toast
import id.abtech.xlinxutil.xray.util.AngConfigManager
import id.abtech.xlinxutil.xray.util.MessageUtil
import id.abtech.xlinxutil.xray.util.Utils
import id.abtech.xlinxutil.xray.util.V2rayConfigUtil
import kotlinx.coroutines.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val isRunning by lazy { MutableLiveData<Boolean>() }
    val updateListAction by lazy { MutableLiveData<Int>() }
    val updateTestResultAction by lazy { MutableLiveData<String>() }

    private val tcpingTestScope by lazy { CoroutineScope(Dispatchers.IO) }

    fun startListenBroadcast() {
        isRunning.value = false
        getApplication<AngApplication>().registerReceiver(mMsgReceiver, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))
        MessageUtil.sendMsg2Service(getApplication(), AppConfig.MSG_REGISTER_CLIENT, "")
    }

    override fun onCleared() {
        getApplication<AngApplication>().unregisterReceiver(mMsgReceiver)
        Log.i(AppConfig.ANG_PACKAGE, "Main ViewModel is cleared")
        super.onCleared()
    }

    fun testAllTcping() {
        tcpingTestScope.coroutineContext[Job]?.cancelChildren()
        Utils.closeAllTcpSockets()
        for (k in 0 until AngConfigManager.configs.vmess.count()) {
            AngConfigManager.configs.vmess[k].testResult = ""
            updateListAction.value = -1 // update all
        }
        for (k in 0 until AngConfigManager.configs.vmess.count()) {
            var serverAddress = AngConfigManager.configs.vmess[k].address
            var serverPort = AngConfigManager.configs.vmess[k].port
            if (AngConfigManager.configs.vmess[k].configType == EConfigType.CUSTOM.value) {
                val serverOutbound = V2rayConfigUtil.getCustomConfigServerOutbound(getApplication(),
                        AngConfigManager.configs.vmess[k].guid) ?: continue
                serverAddress = serverOutbound.getServerAddress() ?: continue
                serverPort = serverOutbound.getServerPort() ?: continue
            }
            tcpingTestScope.launch {
                AngConfigManager.configs.vmess.getOrNull(k)?.let {  // check null in case array is modified during testing
                    it.testResult = Utils.tcping(serverAddress, serverPort)
                    launch(Dispatchers.Main) {
                        updateListAction.value = k
                    }
                }
            }
        }
    }

    fun testCurrentServerRealPing() {
        val socksPort = 10808//Utils.parseInt(defaultDPreference.getPrefString(SettingsActivity.PREF_SOCKS_PORT, "10808"))
        GlobalScope.launch(Dispatchers.IO) {
            val result = Utils.testConnection(getApplication(), socksPort)
            launch(Dispatchers.Main) {
                updateTestResultAction.value = result
            }
        }
    }

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {
                    isRunning.value = true
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    isRunning.value = false
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    getApplication<AngApplication>().toast(R.string.toast_services_success)
                    isRunning.value = true
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    getApplication<AngApplication>().toast(R.string.toast_services_failure)
                    isRunning.value = false
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    isRunning.value = false
                }
            }
        }
    }
}
