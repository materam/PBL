package com.materam.mindful

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class HomeActivity : Activity() {

    private lateinit var favoritesBox: LinearLayout
    private lateinit var focusBanner: TextView
    private lateinit var hint: TextView
    private lateinit var gestures: GestureDetector
    private var touchStartedOnItem = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        favoritesBox = findViewById(R.id.favorites)
        focusBanner = findViewById(R.id.focusBanner)
        hint = findViewById(R.id.hint)

        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val start = e1 ?: return false
                val dy = e2.y - start.y
                val dx = e2.x - start.x
                if (dy < -120 && abs(dy) > abs(dx)) {
                    openDrawer()
                    return true
                }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                if (!touchStartedOnItem) {
                    startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
                }
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            touchStartedOnItem = isOverFavorite(ev.rawX.toInt(), ev.rawY.toInt())
        }
        gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun isOverFavorite(x: Int, y: Int): Boolean {
        val rect = Rect()
        for (i in 0 until favoritesBox.childCount) {
            val child = favoritesBox.getChildAt(i)
            if (child.isClickable && child.getGlobalVisibleRect(rect) && rect.contains(x, y)) return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val prefs = Prefs(this)
        val apps = Apps.load(this).associateBy { it.pkg }

        favoritesBox.removeAllViews()
        val favorites = prefs.favorites.mapNotNull { apps[it] }
        if (favorites.isEmpty()) {
            favoritesBox.addView(text(
                "Your home is empty.\nSwipe up, then hold an app to add it here.",
                16f, R.color.muted, clickable = false,
            ))
        } else {
            favorites.forEach { app ->
                val row = text(app.label, 28f, R.color.fg, clickable = true)
                row.setOnClickListener { Apps.open(this, app) }
                row.setOnLongClickListener {
                    Apps.showMenu(this, app) { render() }
                    true
                }
                favoritesBox.addView(row)
            }
        }

        if (prefs.isFocusTime()) {
            focusBanner.text = "Focus hours · ${prefs.focusLabel()}"
            focusBanner.visibility = View.VISIBLE
        } else {
            focusBanner.visibility = View.GONE
        }
        hint.visibility = if (prefs.onboarded) View.INVISIBLE else View.VISIBLE
    }

    private fun text(value: String, sizeSp: Float, colorRes: Int, clickable: Boolean): TextView =
        TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(getColor(colorRes))
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            val pad = (10 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
            isClickable = clickable
            isFocusable = clickable
        }

    private fun openDrawer() {
        Prefs(this).onboarded = true
        startActivity(Intent(this, DrawerActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.slide_up, R.anim.stay)
    }

    @Deprecated("Home screen ignores back")
    override fun onBackPressed() {
        // The home screen is the bottom of the stack; back does nothing here.
    }
}
