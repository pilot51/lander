package com.pilot51.lander;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.Toast;

import com.pilot51.lander.billing.Billing;

public class Options extends PreferenceActivity implements OnPreferenceClickListener, OnKeyListener, OnSharedPreferenceChangeListener {
	public static final int
		UNLOCK_OFF = 0,
		UNLOCK_PURCHASE = 1,
		UNLOCK_KEY = 2,
		DLG_KEY_ENTRY = 0,
		DLG_KEY_RESULT = 1;
	private Preference
		pDefClassic,
		pDefControls,
		pControls,
		pKeyThrust,
		pKeyLeft,
		pKeyRight,
		pKeyNew,
		pKeyRestart,
		pKeyOptions,
		pPresetImpClassic,
		pPresetImproved,
		selectedPref,
		pModUnlock;
	private CheckBoxPreference
		pImpEndImg,
		pImpLanderAlpha,
		pModRotation,
		pModMapList;
	private PreferenceScreen pScreenMod;
	private Billing billing;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
		pDefClassic = findPreference("DefaultClassic");
		pDefControls = findPreference("DefaultControls");
		pControls = findPreference("Controls");
		pKeyThrust = findPreference("KeyThrust");
		pKeyLeft = findPreference("KeyLeft");
		pKeyRight = findPreference("KeyRight");
		pKeyNew = findPreference("KeyNew");
		pKeyRestart = findPreference("KeyRestart");
		pKeyOptions = findPreference("KeyOptions");
		pPresetImpClassic = findPreference("PresetImpClassic");
		pPresetImproved = findPreference("PresetImproved");
		pImpEndImg = (CheckBoxPreference)findPreference("ImpEndImg");
		pImpLanderAlpha = (CheckBoxPreference)findPreference("ImpLanderAlpha");
		pScreenMod = (PreferenceScreen)findPreference("ScreenMod");
		pModRotation = (CheckBoxPreference)findPreference("ModRotation");
		pModMapList = (CheckBoxPreference)findPreference("ModMapList");
		pModUnlock = findPreference("ModUnlock");
		pDefClassic.setOnPreferenceClickListener(this);
		pDefControls.setOnPreferenceClickListener(this);
		pControls.setOnPreferenceClickListener(this);
		pKeyThrust.setOnPreferenceClickListener(this);
		pKeyLeft.setOnPreferenceClickListener(this);
		pKeyRight.setOnPreferenceClickListener(this);
		pKeyNew.setOnPreferenceClickListener(this);
		pKeyRestart.setOnPreferenceClickListener(this);
		pKeyOptions.setOnPreferenceClickListener(this);
		pPresetImpClassic.setOnPreferenceClickListener(this);
		pPresetImproved.setOnPreferenceClickListener(this);
		pScreenMod.setOnPreferenceClickListener(this);
		pModUnlock.setOnPreferenceClickListener(this);
	}
	
	public boolean onPreferenceClick(Preference preference) {
		if (preference == pDefClassic) {
			((SeekBarPreference)findPreference("Gravity")).setToDefault();
			((SeekBarPreference)findPreference("Fuel")).setToDefault();
			((SeekBarPreference)findPreference("Thrust")).setToDefault();
			((CheckBoxPreference)findPreference("DrawFlame")).setChecked(true);
			((CheckBoxPreference)findPreference("ReverseSideThrust")).setChecked(false);
			Toast.makeText(this, R.string.classic_options_reset, Toast.LENGTH_LONG).show();
		} else if (preference == pDefControls) {
			((SeekBarPreference)findPreference("BtnScale")).setToDefault();
			((SeekBarPreference)findPreference("BtnAlpha")).setToDefault();
			Main.prefs.edit()
			.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
			.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
			.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
			.putInt("KeyNew", KeyEvent.KEYCODE_2)
			.putInt("KeyRestart", KeyEvent.KEYCODE_3)
			.putInt("KeyOptions", KeyEvent.KEYCODE_4)
			.commit();
			setResult(1);
			Toast.makeText(this, R.string.controls_reset, Toast.LENGTH_LONG).show();
		} else if (preference == pControls) {
			if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS) {
				findPreference("CatKeys").setEnabled(false);
				Toast.makeText(this, R.string.controls_nokeys, Toast.LENGTH_LONG).show();
			}
		} else if (preference.getKey().startsWith("Key")) {
			selectedPref = preference;
			assignKey();
		} else if (preference == pPresetImpClassic) {
			pImpEndImg.setChecked(false);
			pImpLanderAlpha.setChecked(false);
		} else if (preference == pPresetImproved) {
			pImpEndImg.setChecked(true);
			pImpLanderAlpha.setChecked(true);
		} else if (preference == pModUnlock) {
			if (Main.prefs.getInt("unlock", UNLOCK_OFF) == UNLOCK_OFF) {
				new AlertDialog.Builder(this)
					.setItems(R.array.unlock_choices, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							switch (item) {
								case 0: // Purchase
									for(int i = 0; !Billing.bReady & i < 10; i++) {
										try {
											Thread.sleep(500);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
									}
									if (Billing.bReady)
										billing.purchase();
									break;
								case 1: // Enter key
									createDialog(0, null);
									break;
							}
						}
					}).show();
			}
		} else if (preference == pScreenMod) {
			if (Main.prefs.getInt("unlock", UNLOCK_OFF) == UNLOCK_KEY) {
				if (!verifyUnlockKey(Main.prefs.getString("unlockKey", null)))
					Main.prefs.edit().putInt("unlock", UNLOCK_OFF).commit();
			}
			if (Main.prefs.getInt("unlock", UNLOCK_OFF) != UNLOCK_KEY)
				billing = new Billing(this);
			updateUnlock();
		}
		return true;
	}
	
	private void createDialog(final int id, final String key) {
		switch (id) {
			case DLG_KEY_ENTRY:
				final EditText input = new EditText(this);
				// Restrict text entry to letters and digits
				input.setFilters(new InputFilter[] {new InputFilter() {
					public CharSequence filter(CharSequence source, int start, int end,
						Spanned dest, int dstart, int dend) {
						for (int i = start; i < end; i++) {
							if (!Character.isLetterOrDigit(source.charAt(i)))
								return "";
						}
						return null;
					}
				}});
				if (key != null) input.setText(key);
				AlertDialog.Builder dlgKeyEntry = new AlertDialog.Builder(this);
				dlgKeyEntry
					.setTitle(R.string.key_entry_title)
					.setMessage(R.string.key_entry_msg)
					.setView(input)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							createDialog(DLG_KEY_RESULT, input.getText().toString());
						}
					})
					.setNeutralButton(R.string.key_entry_keyreq,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								Intent email = new Intent(Intent.ACTION_SEND);
								email.setType("plain/text");
								email.putExtra(Intent.EXTRA_EMAIL, new String[] {getString(R.string.dev_email)});
								email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.key_req_subject));
								email.putExtra(Intent.EXTRA_TEXT, getString(R.string.key_req_message));
								try {
									startActivity(email);
								} catch (ActivityNotFoundException e) {
									e.printStackTrace();
									Toast.makeText(getBaseContext(), R.string.error_email, Toast.LENGTH_LONG).show();
								}
							}
						})
					.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {}
						});
				dlgKeyEntry.show();
				break;
			case DLG_KEY_RESULT:
				AlertDialog.Builder dlgKeyResult = new AlertDialog.Builder(this)
					.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {}
						});
				if (verifyUnlockKey(key)) {
					dlgKeyResult.setMessage(R.string.key_success);
					Main.prefs.edit()
					.putInt("unlock", UNLOCK_KEY)
					.putString("unlockKey", key)
					.commit();
				} else dlgKeyResult.setMessage(R.string.key_fail).setNegativeButton(
					R.string.key_tryagain, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							createDialog(DLG_KEY_ENTRY, key);
						}
					});
				dlgKeyResult.show();
				break;
		}
	}

	private static boolean verifyUnlockKey(String key) {
		if (key == null) return false;
		if (key.length() == 4 & (Long.parseLong(key, Character.MAX_RADIX) * 1000) % 27322 == 0)
			return true;
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Main.prefs.registerOnSharedPreferenceChangeListener(this);
		updateUnlock();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Main.prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("unlock"))
			updateUnlock();
	}
	
	private void updateUnlock() {
		boolean unlock = Main.prefs.getInt("unlock", UNLOCK_OFF) != UNLOCK_OFF;
		pModRotation.setEnabled(unlock);
		pModMapList.setEnabled(unlock);
		if (unlock)
			pScreenMod.removePreference(pModUnlock);
		else {
			pScreenMod.addPreference(pModUnlock);
			pModRotation.setChecked(false);
			pModMapList.setChecked(false);
		}
	}
	
	private void assignKey() {
		new AlertDialog.Builder(this).setIcon(getResources().getDrawable(R.drawable.icon))
		.setMessage(getString(R.string.current_button) + " " + selectedPref.getTitle() + ":\n" + getKeyLabel(Main.prefs.getInt(selectedPref.getKey(), 0)) + "\n\n" + getString(R.string.press_new_button))
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.setOnKeyListener(this)
		.create().show();
	}
	
	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		if (event.getDeviceId() == 0 & event.getAction() == KeyEvent.ACTION_UP) {
			Main.prefs.edit().putInt(selectedPref.getKey(), keyCode).commit();
			setResult(1);
			dialog.dismiss();
			return true;
		}
		return false;
	}
	
	private String getKeyLabel(int keyCode) {
		KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(0);
		String label;
		int savedKey = Main.prefs.getInt(selectedPref.getKey(), 0);
		if (keyCharacterMap.isPrintingKey(savedKey)) {
			label = Character.toString(keyCharacterMap.getDisplayLabel(savedKey));
		} else if (savedKey <= 6) {
			label = (new String[] { "UNKNOWN", "SOFT LEFT", "SOFT RIGHT", "HOME", "BACK", "CALL", "ENDCALL" })[savedKey];
		} else if (savedKey >= 19 & savedKey <= 28) {
			label = (new String[] { "UP", "DOWN", "LEFT", "RIGHT", "CENTER", "VOLUME UP", "VOLUME DOWN", "POWER", "CAMERA", "CLEAR" })[savedKey - 19];
		} else if (savedKey >= 57 & savedKey <= 67) {
			label = (new String[] { "LEFT ALT", "RIGHT ALT", "LEFT SHIFT", "RIGHT SHIFT", "TAB", "SPACE", "SYM", "EXPLORER", "ENVELOPE", "ENTER", "DEL" })[savedKey - 57];
		} else if (savedKey >= 78 & savedKey <= 91) {
			label = (new String[] { "NUM", "HEADSETHOOK", "FOCUS", "PLUS", "MENU", "NOTIFICATION", "SEARCH", "PLAY/PAUSE", "STOP", "NEXT", "PREVIOUS", "REWIND", "FAST FORWARD", "MUTE" })[savedKey - 78];
		} else {
			label = "(UNKNOWN)";
		}
		return label;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		if (billing != null)
			billing.onStart();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		if (billing != null)
			billing.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (billing != null)
			billing.onDestroy();
	}
}
