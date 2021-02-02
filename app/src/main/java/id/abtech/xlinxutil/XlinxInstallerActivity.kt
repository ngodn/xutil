package id.abtech.xlinxutil

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*

class XlinxInstallerActivity : AppCompatActivity() {

    companion object {
        private const val FOLDER_URL = "https://media.githubusercontent.com/media/ngodn/xutil/master/gcore/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xlinx_installer)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    private fun determineArchName(): String? {
        // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
        // while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
        // Instead we search through the supported abi:s on the device, see:
        // http://developer.android.com/ndk/guides/abis.html
        // Note that we search for abi:s in preferred order (the ordering of the
        // Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
        // emulation is available.
        for (androidArch in Build.SUPPORTED_ABIS) {
            when (androidArch) {
                "arm64-v8a" -> return "arm64"
                "armeabi-v7a" -> return "arm"
                "x86_64" -> return "x86-64"
                "x86" -> return "i686"
            }
        }
        throw RuntimeException(
            "Unable to determine arch from Build.SUPPORTED_ABIS =  " +
                    Arrays.toString(Build.SUPPORTED_ABIS)
        )
    }

    private fun determineMinimumAPI(): String? {
        return Build.VERSION.SDK_INT.toString()
    }

    private fun buildURL(apktype: String): String {
        var completeURL = ""

        when (apktype) {
            "gms" -> {
                completeURL =  FOLDER_URL + "gms_arm_" + "minAPI" + determineMinimumAPI() + ".apk"
            }
        }
    }

}