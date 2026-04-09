package com.assistant.mlbb.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.assistant.mlbb.R
import com.assistant.mlbb.databinding.LayoutFabBinding
import com.assistant.mlbb.databinding.LayoutOverlayPanelBinding

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var fabBinding: LayoutFabBinding
    private lateinit var panelBinding: LayoutOverlayPanelBinding

    private var isPanelVisible = false

    companion object {
        private var instance: OverlayService? = null

        fun updateDetectedHeroes(detected: Map<String, String>) {
            instance?.updateUI(detected)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun setupFab() {
        fabBinding = LayoutFabBinding.inflate(LayoutInflater.from(this))
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        fabBinding.root.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(fabBinding.root, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            togglePanel()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(fabBinding.root, params)
    }

    private fun setupPanel() {
        panelBinding = LayoutOverlayPanelBinding.inflate(LayoutInflater.from(this))
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        panelBinding.root.visibility = View.GONE
        windowManager.addView(panelBinding.root, params)
    }

    private fun togglePanel() {
        isPanelVisible = !isPanelVisible
        panelBinding.root.visibility = if (isPanelVisible) View.VISIBLE else View.GONE
    }

    private var counters: Map<String, List<String>> = emptyMap()

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        loadCounters()
        setupFab()
        setupPanel()
    }

    private fun loadCounters() {
        try {
            val jsonString = assets.open("counters.json").bufferedReader().use { it.readText() }
            val type = object : com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.type
            counters = com.google.gson.Gson().fromJson(jsonString, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateUI(detected: Map<String, String>) {
        val detectedList = detected.values.distinct()
        if (detectedList.isEmpty()) {
            panelBinding.tvDetected.text = "Detecting enemy picks..."
            return
        }

        panelBinding.tvDetected.text = "Enemy: ${detectedList.joinToString(", ")}"
        
        // Find best counters (most frequent counters for the detected enemies)
        val allCounters = mutableListOf<String>()
        detectedList.forEach { hero ->
            counters[hero]?.let { allCounters.addAll(it) }
        }

        val topCounters = allCounters.groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { (_, count) -> count }
            .take(3)
            .map { it.first }

        panelBinding.llSuggestions.removeAllViews()
        topCounters.forEach { counterHero ->
            val tv = TextView(this).apply {
                text = counterHero
                setTextColor(android.graphics.Color.WHITE)
                setPadding(16, 8, 16, 8)
                setBackgroundResource(R.drawable.bg_pill)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(8, 0, 8, 0) }
            }
            panelBinding.llSuggestions.addView(tv)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(fabBinding.root)
        windowManager.removeView(panelBinding.root)
        instance = null
    }
}
