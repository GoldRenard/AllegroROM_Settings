package com.goldrenard.allegrosettings;

import android.content.ContentResolver;
import android.provider.Settings;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import java.util.List;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String KEY_STATUS_BAR_BATTERY_ = "status_bar_battery";
    private static final String KEY_VOLUME_OVERLAY = "volume_overlay";
    private static final String KEY_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String KEY_KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";

    private static final String PREF_STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String PREF_MODE_VOLUME_OVERLAY = "volume_overlay_mode";
    private static final String PREF_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String PREF_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String PREF_KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";

    private ListPreference mStatusBarBattery;
    private ListPreference mVolumeOverlay;
    private CheckBoxPreference mVolumeAdjustSounds;
    private CheckBoxPreference mVolBtnMusicCtrl;
    private CheckBoxPreference mKillAppLongpressBack;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        SetCurrentSettings();
    }

    private void SetCurrentSettings()
    {
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getBaseContext().getContentResolver();

        int statusBarBattery = Settings.System.getInt(resolver, PREF_STATUS_BAR_BATTERY, 0);
        int volumeOverlay = Settings.System.getInt(getContentResolver(), PREF_MODE_VOLUME_OVERLAY, 0);
        int volbtnmusic = Settings.System.getInt(resolver, PREF_VOLBTN_MUSIC_CTRL, 1);
        int voladjustsounds = Settings.System.getInt(resolver, PREF_VOLUME_ADJUST_SOUNDS, 1);
        int killlongpressback = Settings.System.getInt(resolver, PREF_KILL_APP_LONGPRESS_BACK, 0);


        mStatusBarBattery = (ListPreference)prefSet.findPreference(KEY_STATUS_BAR_BATTERY_);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mVolumeOverlay = (ListPreference)findPreference(KEY_VOLUME_OVERLAY);
        mVolumeOverlay.setOnPreferenceChangeListener(this);
        mVolumeOverlay.setValue(Integer.toString(volumeOverlay));
        mVolumeOverlay.setSummary(mVolumeOverlay.getEntry());

        mVolBtnMusicCtrl = (CheckBoxPreference)findPreference(KEY_VOLBTN_MUSIC_CTRL);
        mVolBtnMusicCtrl.setChecked(volbtnmusic != 0);
        mVolBtnMusicCtrl.setOnPreferenceChangeListener(this);

        mVolumeAdjustSounds = (CheckBoxPreference)findPreference(KEY_VOLUME_ADJUST_SOUNDS);
        mVolumeAdjustSounds.setChecked(voladjustsounds != 0);
        mVolumeAdjustSounds.setOnPreferenceChangeListener(this);

        mKillAppLongpressBack = (CheckBoxPreference)findPreference(KEY_KILL_APP_LONGPRESS_BACK);
        mKillAppLongpressBack.setChecked(killlongpressback != 0);
        mKillAppLongpressBack.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getBaseContext().getContentResolver();
        if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, PREF_STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            return true;
        }
        else if (preference == mVolumeOverlay) {
            final int value = Integer.valueOf((String)newValue);
            final int index = mVolumeOverlay.findIndexOfValue((String)newValue);
            Settings.System.putInt(getContentResolver(), PREF_MODE_VOLUME_OVERLAY, value);
            mVolumeOverlay.setSummary(mVolumeOverlay.getEntries()[index]);
            return true;
        } else if (preference == mVolBtnMusicCtrl) {
            Settings.System.putInt(getContentResolver(), PREF_VOLBTN_MUSIC_CTRL, (Boolean)newValue ? 1 : 0);
            return true;
        } else if (preference == mVolumeAdjustSounds) {
            Settings.System.putInt(getContentResolver(), PREF_VOLUME_ADJUST_SOUNDS, (Boolean)newValue ? 1 : 0);
            return true;
        }  else if (preference == mKillAppLongpressBack) {
            Settings.System.putInt(getContentResolver(), PREF_KILL_APP_LONGPRESS_BACK, (Boolean)newValue ? 1 : 0);
            return true;
        }
        return false;
    }
}
