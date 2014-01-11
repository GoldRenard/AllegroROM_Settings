
// IntelliJ API Decompiler stub source generated from a class file
// Implementation of methods is not available

package android.media;

public class AudioManager2 {
    public static final java.lang.String ACTION_AUDIO_BECOMING_NOISY = "android.media.AUDIO_BECOMING_NOISY";
    public static final java.lang.String RINGER_MODE_CHANGED_ACTION = "android.media.RINGER_MODE_CHANGED";
    public static final java.lang.String EXTRA_RINGER_MODE = "android.media.EXTRA_RINGER_MODE";
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final java.lang.String VIBRATE_SETTING_CHANGED_ACTION = "android.media.VIBRATE_SETTING_CHANGED";
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final java.lang.String EXTRA_VIBRATE_SETTING = "android.media.EXTRA_VIBRATE_SETTING";
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final java.lang.String EXTRA_VIBRATE_TYPE = "android.media.EXTRA_VIBRATE_TYPE";
    public static final int STREAM_VOICE_CALL = 0;
    public static final int STREAM_SYSTEM = 1;
    public static final int STREAM_RING = 2;
    public static final int STREAM_MUSIC = 3;
    public static final int STREAM_ALARM = 4;
    public static final int STREAM_NOTIFICATION = 5;
    public static final int STREAM_DTMF = 8;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int NUM_STREAMS = 5;
    public static final int ADJUST_RAISE = 1;
    public static final int ADJUST_LOWER = -1;
    public static final int ADJUST_SAME = 0;
    public static final int FLAG_SHOW_UI = 1;
    public static final int FLAG_ALLOW_RINGER_MODES = 2;
    public static final int FLAG_PLAY_SOUND = 4;
    public static final int FLAG_REMOVE_SOUND_AND_VIBRATE = 8;
    public static final int FLAG_VIBRATE = 16;
    public static final int RINGER_MODE_SILENT = 0;
    public static final int RINGER_MODE_VIBRATE = 1;
    public static final int RINGER_MODE_NORMAL = 2;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int VIBRATE_TYPE_RINGER = 0;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int VIBRATE_TYPE_NOTIFICATION = 1;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int VIBRATE_SETTING_OFF = 0;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int VIBRATE_SETTING_ON = 1;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int VIBRATE_SETTING_ONLY_SILENT = 2;
    public static final int USE_DEFAULT_STREAM_TYPE = -2147483648;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final java.lang.String ACTION_SCO_AUDIO_STATE_CHANGED = "android.media.SCO_AUDIO_STATE_CHANGED";
    public static final java.lang.String ACTION_SCO_AUDIO_STATE_UPDATED = "android.media.ACTION_SCO_AUDIO_STATE_UPDATED";
    public static final java.lang.String EXTRA_SCO_AUDIO_STATE = "android.media.extra.SCO_AUDIO_STATE";
    public static final java.lang.String EXTRA_SCO_AUDIO_PREVIOUS_STATE = "android.media.extra.SCO_AUDIO_PREVIOUS_STATE";
    public static final int SCO_AUDIO_STATE_DISCONNECTED = 0;
    public static final int SCO_AUDIO_STATE_CONNECTED = 1;
    public static final int SCO_AUDIO_STATE_CONNECTING = 2;
    public static final int SCO_AUDIO_STATE_ERROR = -1;
    public static final int MODE_INVALID = -2;
    public static final int MODE_CURRENT = -1;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_RINGTONE = 1;
    public static final int MODE_IN_CALL = 2;
    public static final int MODE_IN_COMMUNICATION = 3;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_EARPIECE = 1;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_SPEAKER = 2;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_BLUETOOTH = 4;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_BLUETOOTH_SCO = 4;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_HEADSET = 8;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_BLUETOOTH_A2DP = 16;
    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public static final int ROUTE_ALL = -1;
    public static final int FX_KEY_CLICK = 0;
    public static final int FX_FOCUS_NAVIGATION_UP = 1;
    public static final int FX_FOCUS_NAVIGATION_DOWN = 2;
    public static final int FX_FOCUS_NAVIGATION_LEFT = 3;
    public static final int FX_FOCUS_NAVIGATION_RIGHT = 4;
    public static final int FX_KEYPRESS_STANDARD = 5;
    public static final int FX_KEYPRESS_SPACEBAR = 6;
    public static final int FX_KEYPRESS_DELETE = 7;
    public static final int FX_KEYPRESS_RETURN = 8;
    public static final int AUDIOFOCUS_GAIN = 1;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT = 2;
    public static final int AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK = 3;
    public static final int AUDIOFOCUS_LOSS = -1;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT = -2;
    public static final int AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK = -3;
    public static final int AUDIOFOCUS_REQUEST_FAILED = 0;
    public static final int AUDIOFOCUS_REQUEST_GRANTED = 1;
    public static final java.lang.String PROPERTY_OUTPUT_SAMPLE_RATE = "android.media.property.OUTPUT_SAMPLE_RATE";
    public static final java.lang.String PROPERTY_OUTPUT_FRAMES_PER_BUFFER = "android.media.property.OUTPUT_FRAMES_PER_BUFFER";

    AudioManager2() { /* compiled code */ }

    public void adjustStreamVolume(int streamType, int direction, int flags) { /* compiled code */ }

    public void adjustVolume(int direction, int flags) { /* compiled code */ }

    public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags) { /* compiled code */ }

    public int getRingerMode() { /* compiled code */ return 0; }

    public int getStreamMaxVolume(int streamType) { /* compiled code */return 0; }

    public int getStreamVolume(int streamType) { /* compiled code */return 0; }

    public void setRingerMode(int ringerMode) { /* compiled code */ }

    public void setStreamVolume(int streamType, int index, int flags) { /* compiled code */ }

    public void setStreamSolo(int streamType, boolean state) { /* compiled code */ }

    public void setStreamMute(int streamType, boolean state) { /* compiled code */ }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public boolean shouldVibrate(int vibrateType) { /* compiled code */return false; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public int getVibrateSetting(int vibrateType) { /* compiled code */return 0; }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void setVibrateSetting(int vibrateType, int vibrateSetting) { /* compiled code */ }

    public void setSpeakerphoneOn(boolean on) { /* compiled code */ }

    public boolean isSpeakerphoneOn() { /* compiled code */return false;}

    public boolean isBluetoothScoAvailableOffCall() { /* compiled code */return true; }

    public void startBluetoothSco() { /* compiled code */ }

    public void stopBluetoothSco() { /* compiled code */ }

    public void setBluetoothScoOn(boolean on) { /* compiled code */ }

    public boolean isBluetoothScoOn() { /* compiled code */ return true;}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void setBluetoothA2dpOn(boolean on) { /* compiled code */ }

    public boolean isBluetoothA2dpOn() { /* compiled code */ return false;}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void setWiredHeadsetOn(boolean on) { /* compiled code */ }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public boolean isWiredHeadsetOn() { /* compiled code */ return false;}

    public void setMicrophoneMute(boolean on) { /* compiled code */ }

    public boolean isMicrophoneMute() { /* compiled code */ return false;}

    public void setMode(int mode) { /* compiled code */ }

    public int getMode() { /* compiled code */ return 0;}

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public void setRouting(int mode, int routes, int mask) { /* compiled code */ }

    /**
     * @deprecated
     */
    @java.lang.Deprecated
    public int getRouting(int mode) { /* compiled code */return 0; }

    public boolean isMusicActive() { /* compiled code */ return false;}

    public void setParameters(java.lang.String keyValuePairs) { /* compiled code */ }

    public java.lang.String getParameters(java.lang.String keys) { /* compiled code */ return "test";}

    public void playSoundEffect(int effectType) { /* compiled code */ }

    public void playSoundEffect(int effectType, float volume) { /* compiled code */ }

    public void loadSoundEffects() { /* compiled code */ }

    public void unloadSoundEffects() { /* compiled code */ }

    public int requestAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l, int streamType, int durationHint) { /* compiled code */return 0; }

    public int abandonAudioFocus(android.media.AudioManager.OnAudioFocusChangeListener l) { /* compiled code */ return 0;}

    public void registerMediaButtonEventReceiver(android.content.ComponentName eventReceiver) { /* compiled code */ }

    public void registerMediaButtonEventReceiver(android.app.PendingIntent eventReceiver) { /* compiled code */ }

    public void unregisterMediaButtonEventReceiver(android.content.ComponentName eventReceiver) { /* compiled code */ }

    public void unregisterMediaButtonEventReceiver(android.app.PendingIntent eventReceiver) { /* compiled code */ }

    public void registerRemoteControlClient(android.media.RemoteControlClient rcClient) { /* compiled code */ }

    public void unregisterRemoteControlClient(android.media.RemoteControlClient rcClient) { /* compiled code */ }

    public java.lang.String getProperty(java.lang.String key) { /* compiled code */return "ete"; }

    public static interface OnAudioFocusChangeListener {
        void onAudioFocusChange(int i);
    }

    public void forceVolumeControlStream(int t) {

    }

    public int getMasterMaxVolume() {
        return  0;
    }

    public int getMasterStreamType() {
        return  0;
    }

    public int getDevicesForStream(int i) {
        return  0;
    }

    public int getMasterVolume() {
        return 0;
    }

    public void setMasterVolume(int i, int g) {
        return;
    }
    public boolean isMasterMute() { return true; }

    public boolean isStreamMute(int i) { return true; }
}