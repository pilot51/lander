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
	private int barMin, barMax;
	private float value, fDefault, min, max;
	private DecimalFormat df = new DecimalFormat("0.##");

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.preference_seekbar);
		String xmlns = "http://schemas.android.com/apk/res/com.pilot51.lander";
		max = attrs.getAttributeFloatValue(xmlns, "max", 0);
		min = attrs.getAttributeFloatValue(xmlns, "min", 0);
		fDefault = attrs.getAttributeFloatValue(xmlns, "defaultValue", 0);
		barMax = convertToBarValue(max);
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
		valueText.setText(df.format(value));
		seekbar.setProgress(convertToBarValue(value));
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
		value = convertToValue(progress >= barMin ? progress : barMin);
		valueText.setText(df.format(value));
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		persistFloat(value);
	}

	private float convertToValue(int value) {
		String title = getTitle().toString();
		if (title.equals(Main.res.getString(R.string.gravity)))
			return min + (value / 3f);
		else if (title.equals(Main.res.getString(R.string.fuel)))
			return min + (value * 100f);
		else if (title.equals(Main.res.getString(R.string.thrust)))
			return min + (value * 1250f);
		else return value;
	}

	private int convertToBarValue(float value) {
		String title = getTitle().toString();
		if (title.equals(Main.res.getString(R.string.gravity)))
			return (int)((value - min) * 3f);
		else if (title.equals(Main.res.getString(R.string.fuel)))
			return (int)((value - min) / 100f);
		else if (title.equals(Main.res.getString(R.string.thrust)))
			return (int)((value - min) / 1250f);
		else return (int)value;
	}
}
