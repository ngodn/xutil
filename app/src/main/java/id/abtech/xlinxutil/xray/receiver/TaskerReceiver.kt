package id.abtech.xlinxutil.xray.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import com.google.zxing.WriterException
import id.abtech.xlinxutil.AppConfig

import id.abtech.xlinxutil.xray.util.Utils

class TaskerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        try {
            val bundle = intent?.getBundleExtra(AppConfig.TASKER_EXTRA_BUNDLE)
            val switch = bundle?.getBoolean(AppConfig.TASKER_EXTRA_BUNDLE_SWITCH, false)
            val guid = bundle?.getString(AppConfig.TASKER_EXTRA_BUNDLE_GUID, "")

            if (switch == null || guid == null || TextUtils.isEmpty(guid)) {
                return
            } else if (switch) {
                if (guid == AppConfig.TASKER_DEFAULT_GUID) {
                    Utils.startVServiceFromToggle(context)
                } else {
                    Utils.startVService(context, guid)
                }
            } else {
                Utils.stopVService(context)
            }
        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }
}
