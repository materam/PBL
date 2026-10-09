package com.materam.mindful

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView

class DrawerActivity : Activity() {

    private lateinit var search: EditText
    private lateinit var list: ListView
    private var all: List<AppEntry> = emptyList()
    private var shown: List<AppEntry> = emptyList()
    private val adapter = AppAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer)
        search = findViewById(R.id.search)
        list = findViewById(R.id.list)
        list.adapter = adapter

        list.setOnItemClickListener { _, _, position, _ -> launch(shown[position]) }
        list.setOnItemLongClickListener { _, _, position, _ ->
            Apps.showMenu(this, shown[position]) { reload() }
            true
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })
        search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO && shown.isNotEmpty()) {
                launch(shown[0])
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        search.setText("")
        reload()
    }

    private fun reload() {
        val hidden = Prefs(this).hidden
        all = Apps.load(this).filter { it.pkg !in hidden }
        applyFilter()
    }

    private fun applyFilter() {
        val q = search.text.toString().trim()
        shown = if (q.isEmpty()) all else all.filter { it.label.contains(q, ignoreCase = true) }
        adapter.notifyDataSetChanged()
    }

    private fun launch(app: AppEntry) {
        Apps.open(this, app)
        finish()
    }

    private inner class AppAdapter : BaseAdapter() {
        override fun getCount(): Int = shown.size
        override fun getItem(position: Int): Any = shown[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = (convertView as? TextView) ?: TextView(this@DrawerActivity).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTextColor(getColor(R.color.fg))
                typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
                val pad = (12 * resources.displayMetrics.density).toInt()
                setPadding(0, pad, 0, pad)
            }
            val app = shown[position]
            if (app.pkg in Prefs(this@DrawerActivity).mindful) {
                val suffix = "   pause"
                val span = SpannableString(app.label + suffix)
                val start = app.label.length
                span.setSpan(ForegroundColorSpan(getColor(R.color.faint)), start, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(RelativeSizeSpan(0.55f), start, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                view.text = span
            } else {
                view.text = app.label
            }
            return view
        }
    }
}
