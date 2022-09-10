package com.pilot51.lander

import android.content.Context
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import java.text.DecimalFormat


class SeekBarPreference(
	context: Context, attrs: AttributeSet
) : Preference(context, attrs), OnSeekBarChangeListener {
	private lateinit var seekbar: SeekBar
	private lateinit var valueText: TextView
	private val sAttrs = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference, 0, 0)
	private val steps = sAttrs.getInt(R.styleable.SeekBarPreference_seekSteps, 0)
	private val fDefault = sAttrs.getFloat(R.styleable.SeekBarPreference_seekDefaultValue, 0f)
	private val min = sAttrs.getFloat(R.styleable.SeekBarPreference_seekMin, 0f)
	private val max = sAttrs.getFloat(R.styleable.SeekBarPreference_seekMax, 0f)
	private val increment = sAttrs.getFloat(R.styleable.SeekBarPreference_seekIncrement, 1f)
	/** Increment as a divisor of 1 */
	private val subIncrement = sAttrs.getFloat(R.styleable.SeekBarPreference_seekSubIncrement, 1f)
	/**
	 * Divide by this to convert actual value to visible bar value, or multiply for the other direction.
	 * Useful, for example, to display a color value as a percentage when the actual range is 0-255.
	 */
	private val scale = sAttrs.getFloat(R.styleable.SeekBarPreference_seekValueScale, 1f)
	private val suffix = sAttrs.getString(R.styleable.SeekBarPreference_seekSuffix) ?: ""
	private val barMin = convertToBarValue(min)
	private val barMax = if (steps > 1) steps else convertToBarValue(max)
	private val df = DecimalFormat("0.##")
	private var value = 0f

	init {
		layoutResource = R.layout.preference_seekbar
		isPersistent = true
		setDefaultValue(fDefault)
	}

	@Deprecated("Deprecated in Java")
	override fun onSetInitialValue(restorePersistedValue: Boolean, defaultValue: Any?) {
		value = getPersistedFloat(fDefault)
		if (!restorePersistedValue) persistFloat(value)
	}

	override fun onBindViewHolder(holder: PreferenceViewHolder) {
		super.onBindViewHolder(holder)
		seekbar = holder.findViewById(R.id.seekbar) as SeekBar
		seekbar.max = barMax
		seekbar.setOnSeekBarChangeListener(this)
		valueText = holder.findViewById(R.id.value) as TextView
		valueText.text = df.format(value / scale.toDouble()) + suffix
		seekbar.progress = convertToBarValue(value)
	}

	override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromTouch: Boolean) {
		value = convertToValue(if (progress >= barMin) progress else barMin)
		valueText.text = df.format(value / scale.toDouble()) + suffix
	}

	override fun onStartTrackingTouch(seekBar: SeekBar) {}

	override fun onStopTrackingTouch(seekBar: SeekBar) {
		persistFloat(value)
	}

	override fun onClick() {
		seekbar.progress = if (seekbar.progress < seekbar.max) {
			seekbar.progress + 1
		} else barMin
		value = convertToValue(seekbar.progress)
		valueText.text = df.format(value / scale.toDouble()) + suffix
		persistFloat(value)
	}

	fun setToDefault() {
		value = fDefault
		valueText.text = df.format(value / scale.toDouble()) + suffix
		seekbar.progress = convertToBarValue(value)
		persistFloat(value)
	}

	private fun convertToValue(value: Int): Float {
		return if (subIncrement > 1) {
			(min + value / subIncrement) * scale
		} else (min + value * increment) * scale
	}

	private fun convertToBarValue(value: Float): Int {
		return if (subIncrement > 1) {
			((value - min) * subIncrement / scale).toInt()
		} else ((value - min) / increment / scale).toInt()
	}
}
