package com.pilot51.lander;

import java.text.DecimalFormat;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
	private SeekBar seekbar;
	private TextView valueText;
	private int steps, barMin, barMax;
	private float value, fDefault, min, max, increment;
	/** Increment as a divisor of 1 */
	private float subIncrement;
	/** Divide by this to convert actual value to visible bar value, or multiply for the other direction.<br />
	 * Useful, for example, to display a color value as a percentage when the actual range is 0-255. */
	private float scale;
	private String suffix;
	private DecimalFormat df = new DecimalFormat("0.##");

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.preference_seekbar);
		String xmlns = "http://schemas.android.com/apk/res/com.pilot51.lander";
		max = attrs.getAttributeFloatValue(xmlns, "max", 0);
		min = attrs.getAttributeFloatValue(xmlns, "min", 0);
		fDefault = attrs.getAttributeFloatValue(xmlns, "defaultValue", 0);
		increment = attrs.getAttributeFloatValue(xmlns, "increment", 1);
		subIncrement = attrs.getAttributeFloatValue(xmlns, "subIncrement", 1);
		steps = attrs.getAttributeIntValue(xmlns, "steps", 0);
		scale = attrs.getAttributeFloatValue(xmlns, "valueScale", 1);
		suffix = attrs.getAttributeValue(xmlns, "suffix");
		if (suffix == null)
			suffix = "";
		if (steps > 1)
			barMax = steps;
		else barMax = convertToBarValue(max);
		barMin = convertToBarValue(min);
		setPersistent(true);
		setDefaultValue(fDefault);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		value = getPersistedFloat(fDefault);
		if (!restorePersistedValue) persistFloat(value);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		seekbar = (SeekBar) view.findViewById(R.id.seekbar);
		seekbar.setMax(barMax);
		seekbar.setOnSeekBarChangeListener(this);
		valueText = (TextView) view.findViewById(R.id.value);
		valueText.setText(df.format(value / scale) + suffix);
		seekbar.setProgress(convertToBarValue(value));
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		value = convertToValue(progress >= barMin ? progress : barMin);
		valueText.setText(df.format(value / scale) + suffix);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		persistFloat(value);
	}
	
	protected void setToDefault() {
		this.value = fDefault;
		valueText.setText(df.format(value / scale) + suffix);
		seekbar.setProgress(convertToBarValue(value));
		persistFloat(value);
	}

	private float convertToValue(int value) {
		if (subIncrement > 1) return (min + (value / subIncrement)) * scale;
		else return (min + (value * increment)) * scale;
	}

	private int convertToBarValue(float value) {
		if (subIncrement > 1) return (int)((value - min) * subIncrement / scale);
		else return (int)((value - min) / increment / scale);
	}
}
