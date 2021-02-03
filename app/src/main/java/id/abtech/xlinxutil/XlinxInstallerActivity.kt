package id.abtech.xlinxutil

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProviders
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import id.abtech.xlinxutil.xray.extension.defaultDPreference
import id.abtech.xlinxutil.xray.extension.toast
import id.abtech.xlinxutil.xray.util.AngConfigManager
import id.abtech.xlinxutil.xray.util.Utils
import id.abtech.xlinxutil.xray.util.V2rayConfigUtil
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.File
import java.io.IOException
import java.util.*


@SuppressLint("SetTextI18n")
class XlinxInstallerActivity : AppCompatActivity() {
    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300

        private const val FOLDER_URL = "https://media.githubusercontent.com/media/ngodn/xutil/master/gcore/"

        private const val PM_PLAYSTORE = "com.android.vending"
        private const val PM_ACM = "com.google.android.gsf.login"
        private const val PM_GSF = "com.google.android.gsf"
        private const val PM_GMS = "com.google.android.gms"
        private const val PM_CONTACT = "com.google.android.syncadapters.contacts"
        private const val PM_CALENDAR = "com.google.android.syncadapters.calendar"
        private const val PM_XLINX = "iig.xlinx.app"

        private const val REQ_INSTALL_APK = 6000
        private const val REQ_INSTALL_GSF = 6001
        private const val REQ_INSTALL_ACM = 6002
        private const val REQ_INSTALL_GMS = 6003
        private const val REQ_INSTALL_CONTACT = 6004
        private const val REQ_INSTALL_CALENDAR = 6005
        private const val REQ_INSTALL_PLAYSTORE = 6006
        private const val REQ_INSTALL_XLINX = 6007


        private const val PACKAGE_INSTALLED_ACTION =
            "id.abtech.xlinxutil.SESSION_API_PACKAGE_INSTALLED"

        private const val REQUEST_CODE_VPN_PREPARE = 0
        private const val REQUEST_SCAN = 1
        private const val REQUEST_FILE_CHOOSER = 2
        private const val REQUEST_SCAN_URL = 3

        private const val QIYOU_CONFIG = "{\\r\\n  \\\"dns\\\": {\\r\\n    \\\"hosts\\\": {\\r\\n      \\\"domain:googleapis.cn\\\": \\\"googleapis.com\\\"\\r\\n    },\\r\\n    \\\"servers\\\": [\\r\\n      \\\"1.1.1.1\\\"\\r\\n    ]\\r\\n  },\\r\\n  \\\"inbounds\\\": [\\r\\n    {\\r\\n      \\\"listen\\\": \\\"127.0.0.1\\\",\\r\\n      \\\"port\\\": 10808,\\r\\n      \\\"protocol\\\": \\\"socks\\\",\\r\\n      \\\"settings\\\": {\\r\\n        \\\"auth\\\": \\\"noauth\\\",\\r\\n        \\\"udp\\\": true,\\r\\n        \\\"userLevel\\\": 8\\r\\n      },\\r\\n      \\\"sniffing\\\": {\\r\\n        \\\"destOverride\\\": [\\r\\n          \\\"http\\\",\\r\\n          \\\"tls\\\"\\r\\n        ],\\r\\n        \\\"enabled\\\": true\\r\\n      },\\r\\n      \\\"tag\\\": \\\"socks\\\"\\r\\n    },\\r\\n    {\\r\\n      \\\"listen\\\": \\\"127.0.0.1\\\",\\r\\n      \\\"port\\\": 10809,\\r\\n      \\\"protocol\\\": \\\"http\\\",\\r\\n      \\\"settings\\\": {\\r\\n        \\\"userLevel\\\": 8\\r\\n      },\\r\\n      \\\"tag\\\": \\\"http\\\"\\r\\n    }\\r\\n  ],\\r\\n  \\\"log\\\": {\\r\\n    \\\"loglevel\\\": \\\"warning\\\"\\r\\n  },\\r\\n  \\\"outbounds\\\": [\\r\\n    {\\r\\n      \\\"mux\\\": {\\r\\n        \\\"concurrency\\\": -1,\\r\\n        \\\"enabled\\\": false\\r\\n      },\\r\\n      \\\"protocol\\\": \\\"vmess\\\",\\r\\n      \\\"settings\\\": {\\r\\n        \\\"vnext\\\": [\\r\\n          {\\r\\n            \\\"address\\\": \\\"qiyou.zayden.me\\\",\\r\\n            \\\"port\\\": 47457,\\r\\n            \\\"users\\\": [\\r\\n              {\\r\\n                \\\"alterId\\\": 64,\\r\\n                \\\"id\\\": \\\"b7204b42-98ac-443c-da19-b2504f2aeb13\\\",\\r\\n                \\\"level\\\": 8,\\r\\n                \\\"security\\\": \\\"auto\\\"\\r\\n              }\\r\\n            ]\\r\\n          }\\r\\n        ]\\r\\n      },\\r\\n      \\\"streamSettings\\\": {\\r\\n        \\\"network\\\": \\\"ws\\\",\\r\\n        \\\"security\\\": \\\"tls\\\",\\r\\n        \\\"tlsSettings\\\": {\\r\\n          \\\"allowInsecure\\\": true,\\r\\n          \\\"serverName\\\": \\\"\\\"\\r\\n        },\\r\\n        \\\"wsSettings\\\": {\\r\\n          \\\"headers\\\": {\\r\\n            \\\"Host\\\": \\\"\\\"\\r\\n          },\\r\\n          \\\"path\\\": \\\"\\/\\\"\\r\\n        }\\r\\n      },\\r\\n      \\\"tag\\\": \\\"proxy\\\"\\r\\n    },\\r\\n    {\\r\\n      \\\"protocol\\\": \\\"freedom\\\",\\r\\n      \\\"settings\\\": {},\\r\\n      \\\"tag\\\": \\\"direct\\\"\\r\\n    },\\r\\n    {\\r\\n      \\\"protocol\\\": \\\"blackhole\\\",\\r\\n      \\\"settings\\\": {\\r\\n        \\\"response\\\": {\\r\\n          \\\"type\\\": \\\"http\\\"\\r\\n        }\\r\\n      },\\r\\n      \\\"tag\\\": \\\"block\\\"\\r\\n    }\\r\\n  ],\\r\\n  \\\"policy\\\": {\\r\\n    \\\"levels\\\": {\\r\\n      \\\"8\\\": {\\r\\n        \\\"connIdle\\\": 300,\\r\\n        \\\"downlinkOnly\\\": 1,\\r\\n        \\\"handshake\\\": 4,\\r\\n        \\\"uplinkOnly\\\": 1\\r\\n      }\\r\\n    },\\r\\n    \\\"system\\\": {\\r\\n      \\\"statsOutboundUplink\\\": true,\\r\\n      \\\"statsOutboundDownlink\\\": true\\r\\n    }\\r\\n  },\\r\\n  \\\"routing\\\": {\\r\\n    \\\"domainStrategy\\\": \\\"IPIfNonMatch\\\",\\r\\n    \\\"rules\\\": []\\r\\n  },\\r\\n  \\\"stats\\\": {}\\r\\n}"

    }

    private lateinit var fetch: Fetch
    private lateinit var request1xGSF: Request
    private lateinit var request2xACM: Request
    private lateinit var request3xGMS: Request
    private lateinit var request4xGConS: Request
    private lateinit var request5xGCalS: Request
    private lateinit var request6xPlayStore: Request
    private lateinit var request7xXlinx: Request

    private val downloadRetryCount: Int = 3
    private var retryCountReq1: Int = 0
    private var retryCountReq2: Int = 0
    private var retryCountReq3: Int = 0
    private var retryCountReq4: Int = 0
    private var retryCountReq5: Int = 0
    private var retryCountReq6: Int = 0


    private lateinit var fullscreenContent: View
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler()

    private lateinit var titleV2Ray: TextView
    private lateinit var titleGoogleServices: TextView
    private lateinit var titleXlinx: TextView
//    private lateinit var statusV2Ray: TextView
//    private lateinit var statusGoogleServices: TextView
//    private lateinit var statusXlinx: TextView
    private lateinit var statusGlobal: TextView
    private lateinit var descV2Ray: TextView
    private lateinit var descXlinx: TextView
    private lateinit var descGSF: TextView
    private lateinit var descACM: TextView
    private lateinit var descGMS: TextView
    private lateinit var descSYNC: TextView
    private lateinit var descPlayStore: TextView
    private lateinit var startButton: Button

    private var isGSFdownloaded: Boolean = false
    private var isACMdownloaded: Boolean = false
    private var isGMSdownloaded: Boolean = false
    private var isContactdownloaded: Boolean = false
    private var isCalendardownloaded: Boolean = false
    private var isPlaystoredownloaded: Boolean = false

    private val mainViewModel: MainViewModel by lazy { ViewModelProviders.of(this).get(MainViewModel::class.java) }

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }
    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreenContent.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fullscreen)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        titleV2Ray = findViewById(R.id.titleV2Ray)
        titleGoogleServices = findViewById(R.id.titleGMS)
        titleXlinx = findViewById(R.id.titleXlinx)
//        statusV2Ray = findViewById(R.id.statusV2Ray)
//        statusGoogleServices = findViewById(R.id.statusGMS)
//        statusXlinx = findViewById(R.id.statusXlinx)
        statusGlobal = findViewById(R.id.statusGlobal)
        descGSF = findViewById(R.id.descGSF)
        descACM = findViewById(R.id.descACM)
        descGMS = findViewById(R.id.descGMS)
        descSYNC = findViewById(R.id.descSYNC)
        descPlayStore = findViewById(R.id.descPlayStore)
        descV2Ray = findViewById(R.id.descV2Ray)
        descXlinx = findViewById(R.id.descXlinx)
        startButton = findViewById(R.id.startButton)


        // Set up the user interaction to manually show or hide the system UI.
        fullscreenContent = findViewById(R.id.fullscreen_content)
        fullscreenContent.setOnClickListener { toggle() }

        fullscreenContentControls = findViewById(R.id.fullscreen_content_controls)

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById<Button>(R.id.dummy_button).setOnTouchListener(delayHideTouchListener)

        request1xGSF = Request(buildURL("gsf"), createFile("gsf").absolutePath)
        request2xACM = Request(buildURL("acm"), createFile("acm").absolutePath)
        request3xGMS = Request(buildURL("gms"), createFile("gms").absolutePath)
        request4xGConS = Request(buildURL("contact"), createFile("contact").absolutePath)
        request5xGCalS = Request(buildURL("calendar"), createFile("calendar").absolutePath)
        request6xPlayStore = Request(buildURL("playstore"), createFile("playstore").absolutePath)
        request7xXlinx = Request(buildURL("xlinx"), createFile("xlinx").absolutePath)

        val pm: PackageManager = this.packageManager

        val fetchConfiguration: FetchConfiguration = FetchConfiguration.Builder(this)
            .setDownloadConcurrentLimit(3)
            .build()
        fetch = Fetch.Impl.getInstance(fetchConfiguration)

        val fetchListener: FetchListener = object : FetchListener {
            override fun onAdded(download: Download) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            text = "Downloading Google Services Framework ..."
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            text = "Downloading Google Account Manager ..."
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            text = "Downloading Google Play Services ..."
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            text = "Downloading Sync Dependencies ..."
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            text = "Downloading Sync Dependencies ..."
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            text = "Downloading Play Store ..."
                        }
                    }
                }
            }

            override fun onCancelled(download: Download) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            text = "Canceled Google Services Framework ..."
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            text = "Canceled Google Account Manager ..."
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            text = "Canceled Google Play Services ..."
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            text = "Canceled Sync Dependencies ..."
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            text = "Canceled Sync Dependencies ..."
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            text = "Canceled Play Store ..."
                        }
                    }
                }
            }

            override fun onCompleted(download: Download) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            text = "Download Completed : Google Services Framework"
                        }
                        isGSFdownloaded = true
                        if (!isPackageInstalled(PM_GSF, pm)) {
                            installPackage("gsf", REQ_INSTALL_GSF)
                        } else {
                            downloadAPK(request2xACM)
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            text = "Download Completed : Google Account Manager"
                        }
                        isACMdownloaded = true
                        if (!isPackageInstalled(PM_ACM, pm)) {
                            installPackage("acm", REQ_INSTALL_ACM)
                        } else {
                            downloadAPK(request3xGMS)
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            text = "Download Completed : Google Play Services"
                        }
                        isGMSdownloaded = true
                        if (!isPackageInstalled(PM_GMS, pm)) {
                            installPackage("gms", REQ_INSTALL_GMS)
                        } else {
                            downloadAPK(request4xGConS)
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            text = "Download Completed : Sync Dependencies"
                        }
                        isContactdownloaded = true
                        if (!isPackageInstalled(PM_CONTACT, pm)) {
                            installPackage("contact", REQ_INSTALL_CONTACT)
                        } else {
                            downloadAPK(request5xGCalS)
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            text = "Download Completed : Sync Dependencies"
                        }
                        isCalendardownloaded = true
                        if (!isPackageInstalled(PM_CALENDAR, pm)) {
                            installPackage("calendar", REQ_INSTALL_CALENDAR)
                        } else {
                            downloadAPK(request6xPlayStore)
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            text = "Download Completed : Play Store"
                        }
                        isPlaystoredownloaded = true
                        if (!isPackageInstalled(PM_PLAYSTORE, pm)) {
                            installPackage("playstore", REQ_INSTALL_PLAYSTORE)
                        }
                    }
                }
            }

            override fun onDeleted(download: Download) {
            }

            override fun onDownloadBlockUpdated(
                download: Download,
                downloadBlock: DownloadBlock,
                totalBlocks: Int
            ) {
            }

            override fun onError(download: Download, error: Error, throwable: Throwable?) {
                when {
                    request1xGSF.id == download.id && retryCountReq1 <= downloadRetryCount -> {
                        descGSF.apply {
                            (download.file + " | " + error.name + " | " + throwable).also { text = it }
                        }
                        retryCountReq1++
                        fetch.resume(request1xGSF.id)
                    }
                    request2xACM.id == download.id && retryCountReq2 <= downloadRetryCount -> {
                        descACM.apply {
                            (download.file + " | " + error.name + " | " + throwable).also { text = it }
                        }
                        retryCountReq2++
                        fetch.resume(request2xACM.id)
                    }
                    request3xGMS.id == download.id && retryCountReq3 <= downloadRetryCount -> {
                        descGMS.apply {
                            (download.file + " | " + error.name + " | " + throwable).also { text = it }
                        }
                        retryCountReq3++
                        fetch.resume(request3xGMS.id)
                    }
                    request4xGConS.id == download.id && retryCountReq4 <= downloadRetryCount -> {
                        descSYNC.apply {
                            (download.file + " | " + error.name + " | " + throwable).also { text = it }
                        }
                        retryCountReq4++
                        fetch.resume(request4xGConS.id)
                    }
                    request5xGCalS.id == download.id && retryCountReq5 <= downloadRetryCount -> {
                        descSYNC.apply {
                            (download.file + " | " + error.name + " | " + throwable).also { text = it }
                        }
                        retryCountReq5++
                        fetch.resume(request5xGCalS.id)
                    }
                    request6xPlayStore.id == download.id && retryCountReq6 <= downloadRetryCount -> {
                        descPlayStore.apply {
                            (download.file + " | " + error.name + " | " + throwable).also { text = it }
                        }
                        retryCountReq6++
                        fetch.resume(request6xPlayStore.id)
                    }
                }
            }

            override fun onPaused(download: Download) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            text = "Download Paused : Google Services Framework ..."
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            text = "Download Paused : Google Account Manager ..."
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            text = "Download Paused : Google Play Services ..."
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            text = "Download Paused : Sync Dependencies ..."
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            text = "Download Paused : Sync Dependencies ..."
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            text = "Download Paused : Play Store ..."
                        }
                    }
                }
            }

            override fun onProgress(
                download: Download,
                etaInMilliSeconds: Long,
                downloadedBytesPerSecond: Long
            ) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            (download.file + " | " + etaInMilliSeconds + " | " + downloadedBytesPerSecond).also { text = it }
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            (download.file + " | " + etaInMilliSeconds + " | " + downloadedBytesPerSecond).also { text = it }
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            (download.file + " | " + etaInMilliSeconds + " | " + downloadedBytesPerSecond).also { text = it }
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            (download.file + " | " + etaInMilliSeconds + " | " + downloadedBytesPerSecond).also { text = it }
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            (download.file + " | " + etaInMilliSeconds + " | " + downloadedBytesPerSecond).also { text = it }
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            (download.file + " | " + etaInMilliSeconds + " | " + downloadedBytesPerSecond).also { text = it }
                        }
                    }
                }
            }

            override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            (download.file + " | Queued").also { text = it }
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            (download.file + " | Queued").also { text = it }
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            (download.file + " | Queued").also { text = it }
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            (download.file + " | Queued").also { text = it }
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            (download.file + " | Queued").also { text = it }
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            (download.file + " | Queued").also { text = it }
                        }
                    }
                }
            }

            override fun onRemoved(download: Download) {
            }

            override fun onResumed(download: Download) {
            }

            override fun onStarted(
                download: Download,
                downloadBlocks: List<DownloadBlock>,
                totalBlocks: Int
            ) {
            }

            override fun onWaitingNetwork(download: Download) {
                when {
                    request1xGSF.id == download.id -> {
                        descGSF.apply {
                            text = "Waiting for Network : Google Services Framework ..."
                        }
                    }
                    request2xACM.id == download.id -> {
                        descACM.apply {
                            text = "Waiting for Network : Google Account Manager ..."
                        }
                    }
                    request3xGMS.id == download.id -> {
                        descGMS.apply {
                            text = "Waiting for Network : Google Play Services ..."
                        }
                    }
                    request4xGConS.id == download.id -> {
                        descSYNC.apply {
                            text = "Waiting for Network : Sync Dependencies ..."
                        }
                    }
                    request5xGCalS.id == download.id -> {
                        descSYNC.apply {
                            text = "Waiting for Network : Sync Dependencies ..."
                        }
                    }
                    request6xPlayStore.id == download.id -> {
                        descPlayStore.apply {
                            text = "Waiting for Network : Play Store ..."
                        }
                    }
                }
            }

        }

        fetch.addListener(fetchListener)

        setupViewModelObserver()

        checkPermissions()

        startButton.setOnClickListener {
//            checkGoogleServices()
//            checkXlinx()
//            winnieThePoo()
//            downloadAPK(request1xGSF)
//            downloadAPK(request2xACM)
//            downloadAPK(request3xGMS)
//            downloadAPK(request4xGConS)
//            downloadAPK(request5xGCalS)
            downloadAPK(request6xPlayStore)
        }
    }

    private fun determineArchName(): String {
        // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
        // while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
        // Instead we search through the supported abi:s on the device, see:
        // http://developer.android.com/ndk/guides/abis.html
        // Note that we search for abi:s in preferred order (the ordering of the
        // Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
        // emulation is available.
        for (androidArch in Build.SUPPORTED_ABIS) {
            when (androidArch) {
                "arm64-v8a" -> return "arm"
                "armeabi-v7a" -> return "arm"
                "x86_64" -> return "x86-64"
                "x86" -> return "x86-64"
            }
        }
        throw RuntimeException(
            "Unable to determine arch from Build.SUPPORTED_ABIS =  " +
                    Arrays.toString(Build.SUPPORTED_ABIS)
        )
    }

    private fun determineMinimumAPI(apktype: String): String {
        return when (apktype) {
            "gsf" -> {
                return when (Build.VERSION.SDK_INT) {
                    19, 20, 21 -> "minAPI19"
                    22 -> "minAPI22"
                    23 -> "minAPI23"
                    24 -> "minAPI24"
                    25 -> "minAPI25"
                    26 -> "minAPI26"
                    27 -> "minAPI27"
                    28 -> "minAPI28"
                    29 -> "minAPI29"
                    30 -> "minAPI30"
                    else -> "all"
                }
            }
            "acm" -> "all"
            "gms" -> {
                return when (Build.VERSION.SDK_INT) {
                    21, 22 -> "minAPI21"
                    23, 24, 25, 26, 27 -> "minAPI23"
                    28 -> "minAPI28"
                    29 -> "minAPI29"
                    30 -> "minAPI30"
                    else -> "all"
                }
            }
            "contact" -> {
                return when (Build.VERSION.SDK_INT) {
                    22 -> "minAPI22"
                    23, 24, 25, 26, 27, 28 -> "minAPI23"
                    29 -> "minAPI29"
                    30 -> "minAPI30"
                    else -> "all"
                }
            }
            "calendar" -> "minAPI21"
            "playstore" -> "minAPI21"
            else ->  "minAPI" + Build.VERSION.SDK_INT.toString()
        }
    }

    private fun buildURL(apktype: String): String {
        return when (apktype) {
            "gsf" -> FOLDER_URL + "gsf_universal_" + determineMinimumAPI(apktype) + ".apk"
            "acm" -> FOLDER_URL + "acm_universal_" + determineMinimumAPI(apktype) + ".apk"
            "gms" -> FOLDER_URL + "gms_x86-64_minAPI30.apk"
//            "gms" -> FOLDER_URL + "gms_" + determineArchName() + "_" + determineMinimumAPI(apktype) + ".apk"
            "contact" -> FOLDER_URL + "gcontsync_universal_" + determineMinimumAPI(apktype) + ".apk"
            "calendar" -> FOLDER_URL + "gcalsync_universal_" + determineMinimumAPI(apktype) + ".apk"
            "playstore" -> FOLDER_URL + "xx_1.apk"
//            "playstore" -> FOLDER_URL + "playstore_universal_" + determineMinimumAPI(apktype) + ".apk"
            "xlinx" -> FOLDER_URL + "xlinx.apk"
            else ->  ""
        }
    }

    private fun buildFilename(apktype: String): String {
        return when (apktype) {
            "gsf" -> "gsf_universal_" + determineMinimumAPI(apktype) + ".apk"
            "acm" -> "acm_universal_" + determineMinimumAPI(apktype) + ".apk"
            "gms" -> "gms_x86-64_minAPI30.apk"
//            "gms" -> "gms_arm_" + determineMinimumAPI(apktype) + ".apk"
            "contact" -> "gcontsync_universal_" + determineMinimumAPI(apktype) + ".apk"
            "calendar" -> "gcalsync_universal_" + determineMinimumAPI(apktype) + ".apk"
            "playstore" -> "xx_1.apk"
//            "playstore" -> "playstore_universal_" + determineMinimumAPI(apktype) + ".apk"
            "xlinx" -> "xlinx.apk"
            else ->  ""
        }
    }


    private fun createFile(apktype: String): File {
//        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), buildFilename(apktype))
//        return File(this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), buildFilename(apktype))
//        return File(Environment.getDownloadCacheDirectory(), buildFilename(apktype))
        return File(this.cacheDir, buildFilename(apktype))
    }

    private fun downloadAPK(request: Request) {
        request.priority = Priority.HIGH
        request.networkType = NetworkType.ALL

        fetch.enqueue(
            request,
            { updatedRequest: Request? -> Log.i("Request : ", updatedRequest.toString()) },
        ) { error: Error? -> Log.i("Request Error : ", error.toString()) }
    }

    private fun checkGoogleServices() {
        val pm: PackageManager = this.packageManager

        if (!isPackageInstalled(PM_GSF, pm)) {
            downloadAPK(request1xGSF)
        } else {
            descGSF.apply {
                text = "Google Services Framework is already installed."
            }
        }

        if (!isPackageInstalled(PM_ACM, pm) && !isPackageInstalled(PM_GSF, pm)) {
            downloadAPK(request2xACM)
        } else {
            descACM.apply {
                text = "Google Account Manager is already installed."
            }
        }

        if (!isPackageInstalled(PM_GMS, pm) && !isPackageInstalled(PM_ACM, pm) && !isPackageInstalled(PM_GSF, pm)) {
            downloadAPK(request3xGMS)
        } else {
            descGMS.apply {
                text = "Google Play Services is already installed."
            }
        }

        if (!isPackageInstalled(PM_CONTACT, pm) && !isPackageInstalled(PM_GMS, pm) && !isPackageInstalled(PM_ACM, pm) && !isPackageInstalled(PM_GSF, pm)) {
            downloadAPK(request4xGConS)
        } else {
            descSYNC.apply {
                text = "Sync Adapters is already installed."
            }
        }

        if (!isPackageInstalled(PM_CALENDAR, pm) && !isPackageInstalled(PM_CONTACT, pm) && !isPackageInstalled(PM_GMS, pm) && !isPackageInstalled(PM_ACM, pm) && !isPackageInstalled(PM_GSF, pm)) {
            downloadAPK(request5xGCalS)
        } else {
            descSYNC.apply {
                text = "Sync Adapters is already installed."
            }
        }

        if (!isPackageInstalled(PM_PLAYSTORE, pm) && !isPackageInstalled(PM_CALENDAR, pm) && !isPackageInstalled(PM_CONTACT, pm) && !isPackageInstalled(PM_GMS, pm) && !isPackageInstalled(PM_ACM, pm) && !isPackageInstalled(PM_GSF, pm)) {
            downloadAPK(request6xPlayStore)
        } else {
            descPlayStore.apply {
                text = "Google Play Store is already installed."
            }
        }
    }

    private fun checkXlinx() {
        val pm: PackageManager = this.packageManager

        if (!isPackageInstalled(PM_XLINX, pm)) {
            downloadAPK(request7xXlinx)
        } else {
            descXlinx.apply {
                text = "X-LinX is already installed. Please backup, sign out, then uninstall before proceeding."
            }
        }
    }

    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun installPackage(apktype: String, requestCode: Int) {
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
        intent.setDataAndType(
            FileProvider.getUriForFile(
                this, this.applicationContext.packageName + ".provider", createFile(apktype)
            ), "application/vnd.android.package-archive"
        )
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, requestCode)
//        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val pm: PackageManager = this.packageManager

        when (requestCode) {
            REQ_INSTALL_GSF -> {
                if (isPackageInstalled(PM_GSF, pm)) {
                    downloadAPK(request2xACM)
                }
            }
            REQ_INSTALL_ACM -> {
                if (isPackageInstalled(PM_ACM, pm)) {
                    downloadAPK(request3xGMS)
                }
            }
            REQ_INSTALL_GMS -> {
                if (isPackageInstalled(PM_GMS, pm)) {
                    downloadAPK(request4xGConS)
                }
            }
            REQ_INSTALL_CONTACT -> {
                if (isPackageInstalled(PM_CONTACT, pm)) {
                    downloadAPK(request5xGCalS)
                }
            }
            REQ_INSTALL_CALENDAR -> {
                if (isPackageInstalled(PM_CALENDAR, pm)) {
                    downloadAPK(request6xPlayStore)
                }
            }
            REQ_INSTALL_PLAYSTORE -> {
//                if (isPackageInstalled(PM_PLAYSTORE, pm)) {
//                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);

    }

    private fun checkPermissions() {
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, REQ_INSTALL_APK, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INSTALL_PACKAGES)
                .setRationale(R.string.app_name)
                .setPositiveButtonText(android.R.string.ok)
                .setNegativeButtonText(android.R.string.cancel)
                .build()
        )
    }

    private fun installPackageSession(apktype: String) {
        //installtion permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                startActivityForResult(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(
                        Uri.parse(
                            String.format(
                                "package:%s",
                                packageName
                            )
                        )
                    ), 1234
                )
            } else {
            }
        }

        //Storage Permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1
            )
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }

        var session: PackageInstaller.Session? = null
        try {
            val packageInstaller = packageManager.packageInstaller
            val params = SessionParams(
                SessionParams.MODE_FULL_INSTALL
            )
            val sessionId = packageInstaller.createSession(params)
            session = packageInstaller.openSession(sessionId)
            addApkToInstallSession(createFile(apktype), session)
            // Create an install status receiver.
            val context: Context = this@XlinxInstallerActivity
            val intent = Intent(context, XlinxInstallerActivity::class.java)
            intent.action = PACKAGE_INSTALLED_ACTION
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            val statusReceiver = pendingIntent.intentSender
            // Commit the session (this will start the installation workflow).
            session.commit(statusReceiver)
        } catch (e: IOException) {
            throw java.lang.RuntimeException("Couldn't install package", e)
        } catch (e: java.lang.RuntimeException) {
            session?.abandon()
            throw e
        }
    }

    @Throws(IOException::class)
    private fun addApkToInstallSession(file: File, session: PackageInstaller.Session) {
        // It's recommended to pass the file size to openWrite(). Otherwise installation may fail
        // if the disk is almost full.
        session.openWrite("package", 0, -1).use { packageInSession ->
            file.inputStream().use { `is` ->
                val buffer = ByteArray(65536)
                var n: Int
                while (`is`.read(buffer).also { n = it } >= 0) {
                    packageInSession.write(buffer, 0, n)
                }
            }
//            assets.open(assetName).use { `is` ->
//                val buffer = ByteArray(16384)
//                var n: Int
//                while (`is`.read(buffer).also { n = it } >= 0) {
//                    packageInSession.write(buffer, 0, n)
//                }
//            }
        }
    }

//    override fun onNewIntent(intent: Intent) {
//        super.onNewIntent(intent)
//        val extras = intent.extras
//        if (PACKAGE_INSTALLED_ACTION.equals(intent.action)) {
//            val status = extras!!.getInt(PackageInstaller.EXTRA_STATUS)
//            val message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE)
//            when (status) {
//                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
//                    // This test app isn't privileged, so the user has to confirm the install.
//                    val confirmIntent = extras[Intent.EXTRA_INTENT] as Intent?
//                    startActivity(confirmIntent)
//                }
//                PackageInstaller.STATUS_SUCCESS -> Toast.makeText(
//                    this,
//                    "Install succeeded!",
//                    Toast.LENGTH_SHORT
//                ).show()
//                PackageInstaller.STATUS_FAILURE, PackageInstaller.STATUS_FAILURE_ABORTED, PackageInstaller.STATUS_FAILURE_BLOCKED, PackageInstaller.STATUS_FAILURE_CONFLICT, PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, PackageInstaller.STATUS_FAILURE_INVALID, PackageInstaller.STATUS_FAILURE_STORAGE -> Toast.makeText(
//                    this,
//                    "Install failed! $status, $message",
//                    Toast.LENGTH_SHORT
//                ).show()
//                else -> Toast.makeText(
//                    this, "Unrecognized status received from installer: $status",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//    }

    private fun setupViewModelObserver() {
//        mainViewModel.updateListAction.observe(this, {
//            val index = it ?: return@observe
//            if (index >= 0) {
//                adapter.updateSelectedItem(index)
//            } else {
//                adapter.updateConfigList()
//            }
//        })
        mainViewModel.updateTestResultAction.observe(this, { descV2Ray.text = it })
        mainViewModel.isRunning.observe(this, {
            val isRunning = it ?: return@observe
//            adapter.changeable = !isRunning
            if (isRunning) {
//                fab.setImageResource(R.drawable.ic_v)
                statusGlobal.text = getString(R.string.connection_connected)
                if (mainViewModel.isRunning.value == true) {
//                    statusGlobal.text = getString(R.string.connection_test_testing)
                    descV2Ray.text = getString(R.string.connection_test_testing)
                    mainViewModel.testCurrentServerRealPing()
                } else {
//                tv_test_state.text = getString(R.string.connection_test_fail)
                }
                downloadAPK(request1xGSF)
            } else {
//                fab.setImageResource(R.drawable.ic_v_idle)
                statusGlobal.text = getString(R.string.connection_not_connected)
            }
//            hideCircle()
        })
        mainViewModel.startListenBroadcast()
    }

    private fun winnieThePoo() {
        importConfigCustomClipboard()

        if (mainViewModel.isRunning.value == true) {
            Utils.stopVService(this)
        } else if (defaultDPreference.getPrefString(AppConfig.PREF_MODE, "VPN") == "VPN") {
            val intent = VpnService.prepare(this)
            if (intent == null) {
                startV2Ray()
            } else {
                startActivityForResult(intent, REQUEST_CODE_VPN_PREPARE)
            }
        } else {
            startV2Ray()
        }
    }

    private fun startV2Ray() {
        if (AngConfigManager.configs.index < 0) {
            return
        }
        statusGlobal.apply {
            text = "Starting X-Ray ..."
        }
//        toast(R.string.toast_services_start)
        if (!Utils.startVService(this, AngConfigManager.configs.index)) {
            statusGlobal.apply {
                text = "Gathering X-Ray status ..."
            }
        }

        if (mainViewModel.isRunning.value == true) {
            statusGlobal.text = getString(R.string.connection_test_testing)
            descV2Ray.text = getString(R.string.connection_test_testing)
            mainViewModel.testCurrentServerRealPing()
        } else {
//                tv_test_state.text = getString(R.string.connection_test_fail)
        }
    }

    fun importConfigCustomClipboard()
            : Boolean {
        try {
//            val configText = Utils.getClipboard(this)
            val qiyouCFG =   Gson().fromJson("{\n" +
                    "  \"dns\": {\n" +
                    "    \"hosts\": {\n" +
                    "      \"domain:googleapis.cn\": \"googleapis.com\"\n" +
                    "    },\n" +
                    "    \"servers\": [\n" +
                    "      \"1.1.1.1\"\n" +
                    "    ]\n" +
                    "  },\n" +
                    "  \"inbounds\": [\n" +
                    "    {\n" +
                    "      \"listen\": \"127.0.0.1\",\n" +
                    "      \"port\": 10808,\n" +
                    "      \"protocol\": \"socks\",\n" +
                    "      \"settings\": {\n" +
                    "        \"auth\": \"noauth\",\n" +
                    "        \"udp\": true,\n" +
                    "        \"userLevel\": 8\n" +
                    "      },\n" +
                    "      \"sniffing\": {\n" +
                    "        \"destOverride\": [\n" +
                    "          \"http\",\n" +
                    "          \"tls\"\n" +
                    "        ],\n" +
                    "        \"enabled\": true\n" +
                    "      },\n" +
                    "      \"tag\": \"socks\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"listen\": \"127.0.0.1\",\n" +
                    "      \"port\": 10809,\n" +
                    "      \"protocol\": \"http\",\n" +
                    "      \"settings\": {\n" +
                    "        \"userLevel\": 8\n" +
                    "      },\n" +
                    "      \"tag\": \"http\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"log\": {\n" +
                    "    \"loglevel\": \"warning\"\n" +
                    "  },\n" +
                    "  \"outbounds\": [\n" +
                    "    {\n" +
                    "      \"mux\": {\n" +
                    "        \"concurrency\": -1,\n" +
                    "        \"enabled\": false\n" +
                    "      },\n" +
                    "      \"protocol\": \"vmess\",\n" +
                    "      \"settings\": {\n" +
                    "        \"vnext\": [\n" +
                    "          {\n" +
                    "            \"address\": \"qiyou.zayden.me\",\n" +
                    "            \"port\": 47457,\n" +
                    "            \"users\": [\n" +
                    "              {\n" +
                    "                \"alterId\": 64,\n" +
                    "                \"id\": \"b7204b42-98ac-443c-da19-b2504f2aeb13\",\n" +
                    "                \"level\": 8,\n" +
                    "                \"security\": \"auto\"\n" +
                    "              }\n" +
                    "            ]\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      },\n" +
                    "      \"streamSettings\": {\n" +
                    "        \"network\": \"ws\",\n" +
                    "        \"security\": \"tls\",\n" +
                    "        \"tlsSettings\": {\n" +
                    "          \"allowInsecure\": true,\n" +
                    "          \"serverName\": \"\"\n" +
                    "        },\n" +
                    "        \"wsSettings\": {\n" +
                    "          \"headers\": {\n" +
                    "            \"Host\": \"\"\n" +
                    "          },\n" +
                    "          \"path\": \"/\"\n" +
                    "        }\n" +
                    "      },\n" +
                    "      \"tag\": \"proxy\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"protocol\": \"freedom\",\n" +
                    "      \"settings\": {},\n" +
                    "      \"tag\": \"direct\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"protocol\": \"blackhole\",\n" +
                    "      \"settings\": {\n" +
                    "        \"response\": {\n" +
                    "          \"type\": \"http\"\n" +
                    "        }\n" +
                    "      },\n" +
                    "      \"tag\": \"block\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"policy\": {\n" +
                    "    \"levels\": {\n" +
                    "      \"8\": {\n" +
                    "        \"connIdle\": 300,\n" +
                    "        \"downlinkOnly\": 1,\n" +
                    "        \"handshake\": 4,\n" +
                    "        \"uplinkOnly\": 1\n" +
                    "      }\n" +
                    "    },\n" +
                    "    \"system\": {\n" +
                    "      \"statsOutboundUplink\": true,\n" +
                    "      \"statsOutboundDownlink\": true\n" +
                    "    }\n" +
                    "  },\n" +
                    "  \"routing\": {\n" +
                    "    \"domainStrategy\": \"IPIfNonMatch\",\n" +
                    "    \"rules\": []\n" +
                    "  },\n" +
                    "  \"stats\": {}\n" +
                    "}", JsonObject::class.java)
            val configText = qiyouCFG.toString()
            if (TextUtils.isEmpty(configText)) {
                toast(R.string.toast_none_data_clipboard)
                return false
            }
            importCustomizeConfig(configText)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun importCustomizeConfig(server: String?) {
        if (server == null) {
            return
        }
        if (!V2rayConfigUtil.isValidConfig(server)) {
            toast(R.string.toast_config_file_invalid)
            return
        }
        val resId = AngConfigManager.importCustomizeConfig(server)
        if (resId > 0) {
            toast(resId)
        } else {
            toast(R.string.toast_success)
//            adapter.updateConfigList()
        }
    }

    private fun installGoogleDependency() {

    }
}