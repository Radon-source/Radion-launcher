package com.example.helloworld

import com.example.helloworld.R
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var clockText: TextView
    private lateinit var appsRowContainer: LinearLayout
    private lateinit var wallpaperVideoView: VideoView
    private val uiHandler = Handler(Looper.getMainLooper())

    private val clockRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            clockText.text = sdf.format(Date())
            uiHandler.postDelayed(this, 10_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockText = findViewById(R.id.clockText)
        appsRowContainer = findViewById(R.id.appsRowContainer)
        wallpaperVideoView = findViewById(R.id.wallpaperVideoView)

        val settingsButton: Button = findViewById(R.id.settingsButton)
        styleFocusableButton(settingsButton)
        settingsButton.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open Settings", Toast.LENGTH_SHORT).show()
            }
        }

        requestStorageAndSetupWallpaper()
        populateInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        uiHandler.removeCallbacks(clockRunnable)
        uiHandler.post(clockRunnable)
        try {
            if (!wallpaperVideoView.isPlaying) {
                wallpaperVideoView.start()
            }
        } catch (_: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(clockRunnable)
    }

    @Deprecated("Keep launcher active on TV home screen")
    override fun onBackPressed() {
        // Stay on Radion Launcher
    }

    private fun requestStorageAndSetupWallpaper() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        } else {
            setupLiveWallpaper()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLiveWallpaper()
        }
    }

    private fun setupLiveWallpaper() {
        try {
            val videoFile = File("/sdcard/Download/wallpaper.mp4")
            if (videoFile.exists() && videoFile.canRead()) {
                wallpaperVideoView.setVideoPath(videoFile.absolutePath)
                wallpaperVideoView.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    wallpaperVideoView.start()
                }
                wallpaperVideoView.setOnErrorListener { _, _, _ ->
                    wallpaperVideoView.visibility = View.GONE
                    true
                }
            } else {
                wallpaperVideoView.visibility = View.GONE
            }
        } catch (_: Exception) {
            wallpaperVideoView.visibility = View.GONE
        }
    }

    private fun populateInstalledApps() {
        appsRowContainer.removeAllViews()
        val pm = packageManager

        try {
            val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }
            val standardIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val combined = (pm.queryIntentActivities(leanbackIntent, 0) +
                pm.queryIntentActivities(standardIntent, 0))
                .distinctBy { it.activityInfo.packageName }
                .sortedBy { it.loadLabel(pm).toString().lowercase(Locale.getDefault()) }

            for (info in combined) {
                val pkgName = info.activityInfo.packageName
                if (pkgName == packageName) continue

                val label = info.loadLabel(pm).toString()
                val tileButton = Button(this).apply {
                    text = label
                    textSize = 18f
                    setTextColor(Color.WHITE)
                    minWidth = 240
                    minHeight = 140
                    isFocusable = true
                    isFocusableInTouchMode = true
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        rightMargin = 20
                    }
                    setOnClickListener {
                        val launchIntent = pm.getLeanbackLaunchIntentForPackage(pkgName)
                            ?: pm.getLaunchIntentForPackage(pkgName)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }
                    }
                }
                styleFocusableButton(tileButton)
                appsRowContainer.addView(tileButton)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading apps: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun styleFocusableButton(view: View) {
        fun applyState(hasFocus: Boolean) {
            val bg = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(if (hasFocus) Color.parseColor("#1D4ED8") else Color.parseColor("#161B22"))
                setStroke(
                    if (hasFocus) 5 else 2,
                    if (hasFocus) Color.parseColor("#60A5FA") else Color.parseColor("#30363D")
                )
            }
            view.background = bg
            val scale = if (hasFocus) 1.06f else 1.0f
            view.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
        }

        applyState(view.hasFocus())
        view.setOnFocusChangeListener { _, hasFocus -> applyState(hasFocus) }
    }
}
