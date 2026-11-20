package com.amica.walkingdistance

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private val background = Color.rgb(2, 6, 23)
    private val cardColor = Color.rgb(15, 23, 42)
    private val buttonColor = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(148, 163, 184)
    private val green = Color.rgb(52, 211, 153)
    private val segments = mutableListOf<Walk>()
    private lateinit var hours: EditText
    private lateinit var minutes: EditText
    private lateinit var seconds: EditText
    private lateinit var speed: EditText
    private lateinit var result: TextView
    private lateinit var details: TextView
    private lateinit var duration: TextView
    private lateinit var summary: TextView
    private lateinit var segmentList: LinearLayout
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background

        val scroll = ScrollView(this).apply {
            setBackgroundColor(this@MainActivity.background)
            fitsSystemWindows = true
            isFillViewport = true
        }
        val content = column().apply { setPadding(dp(16), dp(18), dp(16), dp(24)) }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(text("Walking Pace Converter", 13, green, true))
        val header = row()
        header.addView(text("Time to Miles Calculator", 22, Color.WHITE, true), weight())
        header.addView(button("Reset") {
            setInputs("0", "0", "0", "2.4")
            toast("Reset to 0h 00m 00s at 2.4 mph")
        })
        content.addView(header)

        val resultCard = card(content)
        resultCard.addView(text("DISTANCE COVERED", 12, muted, true))
        result = text("", 36, green, true)
        resultCard.addView(result)
        details = text("", 14, Color.WHITE)
        resultCard.addView(details)

        val inputCard = card(content)
        inputCard.addView(text("Walking Duration", 18, Color.WHITE, true))
        duration = text("", 13, muted)
        inputCard.addView(duration)
        val timeRow = row().apply { gravity = Gravity.TOP }
        hours = timeField(timeRow, "Hours", "0", 1)
        minutes = timeField(timeRow, "Minutes", "40", 5)
        seconds = timeField(timeRow, "Seconds", "0", 10)
        inputCard.addView(timeRow)

        inputCard.addView(text("Quick Time Presets", 14, muted, true))
        addButtonRow(inputCard, listOf("15 min", "30 min", "40 min")) { index ->
            setTime("0", listOf("15", "30", "40")[index], "0")
        }
        addButtonRow(inputCard, listOf("1 hr", "1 hr 30 min", "2 hr 14 min")) { index ->
            val presets = listOf("1" to "0", "1" to "30", "2" to "14")
            setTime(presets[index].first, presets[index].second, "0")
        }

        inputCard.addView(text("Walking Speed (mph)", 14, muted, true))
        speed = numberInput("2.4", "Walking speed in miles per hour")
        inputCard.addView(speed)
        addButtonRow(inputCard, listOf("2.4 mph", "3.0 mph", "3.5 mph")) { index ->
            speed.setText(listOf("2.4", "3.0", "3.5")[index])
        }
        inputCard.addView(button("+ Add to Session Total", true) { addSegment() })
        inputCard.addView(button("Copy Result") { copyResult() })

        val logCard = card(content)
        logCard.addView(text("Session Log", 18, Color.WHITE, true))
        logCard.addView(text("Combine multiple walks to track a running total.", 13, muted))
        summary = text("", 16, green, true)
        logCard.addView(summary)
        clearButton = button("Clear All") {
            segments.clear()
            renderSegments()
            toast("Session log cleared")
        }
        logCard.addView(clearButton)
        segmentList = column()
        logCard.addView(segmentList)

        savedInstanceState?.let { state ->
            setInputs(
                state.getString("hours", "0"),
                state.getString("minutes", "40"),
                state.getString("seconds", "0"),
                state.getString("speed", "2.4")
            )
            val times = state.getDoubleArray("segmentTimes") ?: doubleArrayOf()
            val speeds = state.getDoubleArray("segmentSpeeds") ?: doubleArrayOf()
            times.zip(speeds).forEach { (time, pace) -> segments.add(Walk(time, pace)) }
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateResult()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        }
        listOf(hours, minutes, seconds, speed).forEach { it.addTextChangedListener(watcher) }
        updateResult()
        renderSegments()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("hours", hours.text.toString())
        outState.putString("minutes", minutes.text.toString())
        outState.putString("seconds", seconds.text.toString())
        outState.putString("speed", speed.text.toString())
        outState.putDoubleArray("segmentTimes", segments.map { it.seconds }.toDoubleArray())
        outState.putDoubleArray("segmentSpeeds", segments.map { it.speed }.toDoubleArray())
        super.onSaveInstanceState(outState)
    }

    private fun currentWalk(): Walk? {
        fun value(field: EditText): Double? {
            val raw = field.text.toString()
            if (raw.isBlank()) return 0.0
            val number = raw.replace(',', '.').toDoubleOrNull()
            if (number == null || !number.isFinite() || number < 0) {
                field.error = "Enter a non-negative number"
                return null
            }
            return number
        }
        val h = value(hours) ?: return null
        val m = value(minutes) ?: return null
        val s = value(seconds) ?: return null
        val mph = value(speed) ?: return null
        val total = h * 3600 + m * 60 + s
        if (!total.isFinite() || total > 1e12 || !((total / 3600) * mph * 1.609344).isFinite() ||
            (mph > 0 && 3600 / mph > 1e12)
        ) {
            duration.text = "Values are too large or speed is too small"
            return null
        }
        return Walk(total, mph)
    }

    private fun updateResult() {
        val walk = currentWalk()
        if (walk == null) {
            result.text = "— miles"
            details.text = "Check your inputs"
            return
        }
        result.text = String.format(Locale.US, "%.2f miles", walk.miles)
        details.text = String.format(
            Locale.US, "%.2f km   •   %.2f min   •   %s\n%.4f hr × %.1f mph",
            walk.kilometers, walk.seconds / 60, formatPace(walk.speed),
            walk.seconds / 3600, walk.speed
        )
        duration.text = String.format(Locale.US, "%,.0f total seconds", walk.seconds)
    }

    private fun addSegment() {
        val walk = currentWalk() ?: return
        if (walk.seconds <= 0) {
            toast("Enter a duration greater than 0 seconds")
            return
        }
        if (walk.speed <= 0) {
            toast("Enter a walking speed greater than 0 mph")
            return
        }
        segments.add(walk)
        renderSegments()
        toast(String.format(Locale.US, "Added %.2f miles to session", walk.miles))
    }

    private fun copyResult() {
        val walk = currentWalk() ?: return
        val value = String.format(
            Locale.US, "%s at %.1f mph = %.2f miles",
            formatTime(walk.seconds), walk.speed, walk.miles
        )
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Walking distance", value))
        toast("Copied: $value")
    }

    private fun renderSegments() {
        val totalSeconds = segments.sumOf { it.seconds }
        val totalMiles = segments.sumOf { it.miles }
        summary.text = String.format(
            Locale.US, "Combined Time: %s\nCombined Distance: %.3f miles",
            formatTime(totalSeconds), totalMiles
        )
        clearButton.isEnabled = segments.isNotEmpty()
        segmentList.removeAllViews()
        if (segments.isEmpty()) {
            segmentList.addView(text("No segments added yet.", 13, muted))
        } else {
            segments.forEachIndexed { index, walk ->
                val line = row()
                line.addView(text(
                    String.format(Locale.US, "%s @ %.1f mph\n%.3f mi", formatTime(walk.seconds), walk.speed, walk.miles),
                    14, Color.WHITE
                ), weight())
                line.addView(button("Remove") {
                    segments.removeAt(index)
                    renderSegments()
                }.apply { contentDescription = "Remove segment ${index + 1}" })
                segmentList.addView(line)
            }
        }
    }

    private fun setInputs(h: String, m: String, s: String, mph: String) {
        setTime(h, m, s)
        speed.setText(mph)
    }

    private fun setTime(h: String, m: String, s: String) {
        hours.setText(h)
        minutes.setText(m)
        seconds.setText(s)
    }

    private fun timeField(parent: LinearLayout, label: String, initial: String, step: Int): EditText {
        val box = column()
        box.setPadding(dp(3), dp(4), dp(3), dp(4))
        box.addView(text(label, 13, muted))
        val input = numberInput(initial, label)
        box.addView(input)
        val controls = row()
        listOf(-step, step).forEach { increment ->
            controls.addView(button(if (increment > 0) "+$step" else "-$step") {
                val current = input.text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                val next = (current + increment).coerceAtLeast(0.0)
                input.setText(if (next % 1.0 == 0.0) String.format(Locale.US, "%.0f", next) else next.toString())
            }, weight())
        }
        box.addView(controls)
        parent.addView(box, weight())
        return input
    }

    private fun addButtonRow(parent: LinearLayout, labels: List<String>, onClick: (Int) -> Unit) {
        val line = row()
        labels.forEachIndexed { index, label -> line.addView(button(label) { onClick(index) }, weight()) }
        parent.addView(line)
    }

    private fun numberInput(initial: String, label: String) = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        setSingleLine()
        setTextColor(Color.WHITE)
        setTextSize(20f)
        contentDescription = label
        setText(initial)
        setSelectAllOnFocus(true)
    }

    private fun button(label: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(if (primary) this@MainActivity.background else Color.WHITE)
        textSize = 12f
        setPadding(dp(5), 0, dp(5), 0)
        background = rounded(if (primary) green else buttonColor)
        setOnClickListener {
            action()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(windowToken, 0)
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)
        ).apply { setMargins(dp(2), dp(4), dp(2), dp(4)) }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun card(parent: LinearLayout): LinearLayout {
        val card = column().apply {
            background = rounded(cardColor)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(14), 0, 0) })
        return card
    }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun weight() = LinearLayout.LayoutParams(0, -2, 1f)
    private fun rounded(color: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(12).toFloat() }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
