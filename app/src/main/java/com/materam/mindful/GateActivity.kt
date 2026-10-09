package com.materam.mindful

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The mindful pause: before a "pause" app opens, the user waits a few seconds,
 * writes down why they are opening it, and picks how long the session should be.
 */
class GateActivity : Activity() {

    private lateinit var countdown: TextView
    private lateinit var breath: TextView
    private lateinit var intention: EditText
    private lateinit var openButton: TextView
    private lateinit var sessions: LinearLayout

    private var timer: CountDownTimer? = null
    private var waitDone = false
    private var minutes = 10
    private val options = listOf(5, 10, 15, 30)

    private lateinit var pkg: String
    private lateinit var className: String
    private lateinit var label: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pkg = intent.getStringExtra(EXTRA_PKG) ?: return finish()
        className = intent.getStringExtra(EXTRA_CLASS) ?: return finish()
        label = intent.getStringExtra(EXTRA_LABEL) ?: pkg

        setContentView(R.layout.activity_gate)
        countdown = findViewById(R.id.countdown)
        breath = findViewById(R.id.breath)
        intention = findViewById(R.id.intention)
        openButton = findViewById(R.id.open)
        sessions = findViewById(R.id.sessions)

        val prefs = Prefs(this)
        val focus = prefs.isFocusTime()
        val opens = prefs.opensToday(pkg)

        findViewById<TextView>(R.id.question).text = "Why are you opening $label?"
        findViewById<TextView>(R.id.stats).text = buildString {
            append(
                when (opens) {
                    0 -> "First time today."
                    1 -> "Opened once today."
                    else -> "Opened $opens times today."
                }
            )
            if (focus) append("\nIt's your focus time (${prefs.focusLabel()}).")
        }

        buildSessionChips()
        findViewById<TextView>(R.id.cancel).setOnClickListener { goHome() }
        openButton.setOnClickListener { if (canOpen()) openApp() }
        intention.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = refreshButton()
        })

        // Focus hours make the pause longer, not impossible.
        val seconds = if (focus) maxOf(prefs.pauseSeconds, 30) else prefs.pauseSeconds
        startCountdown(seconds)
        refreshButton()
        askNotificationPermission()
    }

    private fun startCountdown(seconds: Int) {
        countdown.text = seconds.toString()
        timer = object : CountDownTimer(seconds * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val left = ((millisUntilFinished + 999) / 1000).toInt()
                countdown.text = left.toString()
                breath.text = if (left % 8 >= 4) "breathe in…" else "breathe out…"
            }

            override fun onFinish() {
                waitDone = true
                countdown.text = "·"
                breath.text = "still want to?"
                refreshButton()
            }
        }.start()
    }

    private fun buildSessionChips() {
        val density = resources.displayMetrics.density
        options.forEachIndexed { index, value ->
            val chip = TextView(this).apply {
                text = "$value min"
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, (44 * density).toInt(), 1f).apply {
                    if (index > 0) marginStart = (8 * density).toInt()
                }
                setOnClickListener {
                    minutes = value
                    styleChips()
                }
            }
            sessions.addView(chip)
        }
        styleChips()
    }

    private fun styleChips() {
        for (i in 0 until sessions.childCount) {
            val chip = sessions.getChildAt(i) as TextView
            val selected = options[i] == minutes
            chip.setBackgroundResource(if (selected) R.drawable.btn_filled else R.drawable.btn_outline)
            chip.setTextColor(getColor(if (selected) R.color.bg else R.color.fg))
        }
        refreshButton()
    }

    private fun canOpen(): Boolean = waitDone && intention.text.toString().trim().length >= 3

    private fun refreshButton() {
        if (!::openButton.isInitialized) return
        openButton.text = when {
            !waitDone -> "take a moment"
            intention.text.toString().trim().length < 3 -> "write your reason to continue"
            else -> "open $label for $minutes min"
        }
        openButton.alpha = if (canOpen()) 1f else 0.35f
    }

    private fun openApp() {
        val prefs = Prefs(this)
        prefs.recordOpen(pkg)
        prefs.addIntention(label, intention.text.toString(), minutes)
        SessionReceiver.schedule(this, pkg, label, minutes)
        Apps.launchDirect(this, ComponentName(pkg, className))
        finish()
    }

    private fun goHome() {
        startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_CLASS = "cls"
        const val EXTRA_LABEL = "label"
    }
}
