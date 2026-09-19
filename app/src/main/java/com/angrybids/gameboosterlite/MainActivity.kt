package com.angrybids.gameboosterlite

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.boostButton).setOnClickListener { boost() }
        findViewById<Button>(R.id.restoreButton).setOnClickListener { restore() }
        findViewById<Button>(R.id.enableDndButton).setOnClickListener { requestDndAccess() }
        findViewById<Button>(R.id.tipsButton).setOnClickListener { showTips() }
    }

    /**
     * Boost: asks the system to trim background apps' cached processes,
     * and turns on Do Not Disturb (priority only) if permission is granted.
     * Note: killBackgroundProcesses only affects idle/cached background
     * processes - Android itself decides what's safe to trim, the same
     * way most "booster" apps work under the hood.
     */
    private fun boost() {
        val log = StringBuilder()
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager

        var trimmedCount = 0
        val installedApps = pm.getInstalledApplications(0)
        for (appInfo in installedApps) {
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!isSystemApp && appInfo.packageName != packageName) {
                try {
                    am.killBackgroundProcesses(appInfo.packageName)
                    trimmedCount++
                } catch (e: SecurityException) {
                    // some packages can't be touched - that's expected, skip them
                }
            }
        }
        log.append("Asked the system to trim $trimmedCount background apps\n")

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            log.append("Turned on Do Not Disturb (priority mode)\n")
        } else {
            log.append("Do Not Disturb access not granted yet - tap 'Enable Do Not Disturb access' below\n")
        }

        statusText.text = log.toString()
        Toast.makeText(this, "Boost applied", Toast.LENGTH_SHORT).show()
    }

    /** Restore: turns Do Not Disturb back off. Background apps you closed will simply reopen normally when tapped. */
    private fun restore() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            statusText.text = "Restored notifications to normal."
        } else {
            statusText.text = "Nothing to restore - Do Not Disturb access was never granted."
        }
        Toast.makeText(this, "Restored", Toast.LENGTH_SHORT).show()
    }

    private fun requestDndAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun showTips() {
        statusText.text = """
            Manual tips for less lag while gaming:
            - Turn on Airplane mode, then re-enable only Wi-Fi or mobile data (cuts background pings from other apps)
            - Swipe away recent apps manually before starting
            - Lower screen brightness or turn off adaptive brightness
            - Use your phone's built-in Game Mode / Game Space app for deeper system-level optimization - it has permissions this app can't get
        """.trimIndent()
    }
}
