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
	private int barValue, min, max;
	private float value, defaultValue;
	private DecimalFormat df = new DecimalFormat("0.##");

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.preference_seekbar);
		String xmlns = "http://schemas.android.com/apk/res/com.pilot51.lander";
		max = convertToBarValue(attrs.getAttributeFloatValue(xmlns, "max", 0));
		min = convertToBarValue(attrs.getAttributeFloatValue(xmlns, "min", 0));
		defaultValue = attrs.getAttributeFloatValue(xmlns, "defaultValue", 0);
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		setProgress(getInitialValue());
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		setProgress(getInitialValue());
		seekbar = (SeekBar) view.findViewById(R.id.seekbar);
		seekbar.setMax(max);
		seekbar.setProgress(barValue);
		seekbar.setOnSeekBarChangeListener(this);
		valueText = (TextView) view.findViewById(R.id.value);
		valueText.setText(df.format(value));
		setPersistent(true);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		int newValue = seekbar.getProgress();
		if (!callChangeListener(newValue))
			return;
		if (progress >= min) {
			barValue = newValue;
		} else {
			barValue = min;
		}
		value = convertToValue(barValue);
		valueText.setText(df.format(value));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		setProgress(value);
	}

	private void setProgress(float value) {
		this.value = value;
		barValue = convertToBarValue(value);
		persistFloat(value);
		notifyChanged();
	}

	private float getInitialValue() {
		float initialValue = getPersistedFloat(0);
		if (initialValue == 0)
			return defaultValue;
		else
			return initialValue;
	}

	private float convertToValue(int value) {
		String title = getTitle().toString();
		if (title.contentEquals("Gravity"))
			return 1f + (value / 3f);
		else if (title.contentEquals("Fuel"))
			return 100f + (value * 100f);
		else if (title.contentEquals("Thrust"))
			return 2500f + (value * 1250f);
		return 0;
	}

	private int convertToBarValue(float value) {
		String title = getTitle().toString();
		if (title.contentEquals("Gravity"))
			return (int) ((value - 1f) * 3f);
		else if (title.contentEquals("Fuel"))
			return (int) ((value - 100f) / 100f);
		else if (title.contentEquals("Thrust"))
			return (int) ((value - 2500f) / 1250f);
		return 0;
	}
}
