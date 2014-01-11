package android.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface.OnDismissListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.media.AudioManager2;
import android.media.AudioService;
import android.media.AudioSystem;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager2.LayoutParams;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import java.util.HashMap;

public class VolumePanel extends Handler
        implements android.widget.SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private static final String TAG = "VolumePanel";
    private static boolean LOGD = false;

    private static final String PREF_MODE_VOLUME_OVERLAY = "volume_overlay_mode";
    private static final String PREF_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String PREF_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";

    /**
     * The delay before playing a sound. This small period exists so the user
     * can press another key (non-volume keys, too) to have it NOT be audible.
     * <p>
     * PhoneWindow will implement this part.
     */
    public static final int PLAY_SOUND_DELAY = 300;

    /**
     * The delay before vibrating. This small period exists so if the user is
     * moving to silent mode, it will not emit a short vibrate (it normally
     * would since vibrate is between normal mode and silent mode using hardware
     * keys).
     */
    public static final int VIBRATE_DELAY = 300;

    private static final int VIBRATE_DURATION = 300;
    private static final int BEEP_DURATION = 150;
    private static final int MAX_VOLUME = 100;
    private static final int FREE_DELAY = 10000;
    private static final int TIMEOUT_DELAY = 3000;

    private static final int MSG_VOLUME_CHANGED = 0;
    private static final int MSG_FREE_RESOURCES = 1;
    private static final int MSG_PLAY_SOUND = 2;
    private static final int MSG_STOP_SOUNDS = 3;
    private static final int MSG_VIBRATE = 4;
    private static final int MSG_TIMEOUT = 5;
    private static final int MSG_RINGER_MODE_CHANGED = 6;
    private static final int MSG_MUTE_CHANGED = 7;
    private static final int MSG_REMOTE_VOLUME_CHANGED = 8;
    private static final int MSG_REMOTE_VOLUME_UPDATE_IF_SHOWN = 9;
    private static final int MSG_SLIDER_VISIBILITY_CHANGED = 10;
    private static final int MSG_DISPLAY_SAFE_VOLUME_WARNING = 11;

    // Pseudo stream type for master volume
    private static final int STREAM_MASTER = -100;
    // Pseudo stream type for remote volume is defined in -200

    public static final int VOLUME_OVERLAY_SINGLE = 0;
    public static final int VOLUME_OVERLAY_EXPANDABLE = 1;
    public static final int VOLUME_OVERLAY_EXPANDED = 2;
    public static final int VOLUME_OVERLAY_NONE = 3;

    protected Context mContext;
    private AudioManager2 mAudioManager;
    protected AudioService mAudioService;
    private boolean mRingIsSilent;
    private boolean mShowCombinedVolumes;
    private boolean mVoiceCapable;
    private boolean mVolumeLinkNotification;
    private int mCurrentOverlayStyle = -1;

    // True if we want to play tones on the system stream when the master stream is specified.
    private final boolean mPlayMasterStreamTones;

    /** Dialog containing all the sliders */
    private final Dialog mDialog;
    /** Dialog's content view */
    private final View mView;

    /** The visible portion of the volume overlay */
    private final ViewGroup mPanel;
    /** Contains the sliders and their touchable icons */
    private final ViewGroup mSliderGroup;
    /** The button that expands the dialog to show all sliders */
    private final View mMoreButton;
    /** Dummy divider icon that needs to vanish with the more button */
    private final View mDivider;

    /** Currently active stream that shows up at the top of the list of sliders */
    private int mActiveStreamType = -1;
    /** All the slider controls mapped by stream type */
    private HashMap<Integer,StreamControl> mStreamControls;

    private enum StreamResources {
        BluetoothSCOStream(6, 0x10403fd, 0x108029a, 0x108029a, false),
        RingerStream(2, 0x10403fe, 0x108029f, 0x10802a0, true),
        VoiceStream(0, 0x10403ff, 0x108029e, 0x108029e, false),
        AlarmStream(4, 0x10403fa, 0x1080298, 0x1080299, true),
        FMStream(10, 0x2050051, 0x108064c, 0x108064d, false),
        MATVStream(11, 0x2050052, 0x108036a, 0x1080369, false),
        MediaStream(3, 0x1040400, 0x10802a2, 0x10802a3, true),
        NotificationStream(5, 0x1040401, 0x108029c, 0x108029d, true),
        MasterStream(STREAM_MASTER, 0x1040400, 0x10802a2, 0x10802a3, false),
        RemoteStream(-200, 0x1040400, 0x108031a, 0x108030e, false);

        int streamType;
        int descRes;
        int iconRes;
        int iconMuteRes;
        boolean show;

        StreamResources(int streamType, int descRes, int iconRes, int iconMuteRes, boolean show) {
            this.streamType = streamType;
            this.descRes = descRes;
            this.iconRes = iconRes;
            this.iconMuteRes = iconMuteRes;
            this.show = show;
        }
    };

    // List of stream types and their order
    private static final StreamResources[] STREAMS = {
            StreamResources.BluetoothSCOStream,
            StreamResources.RingerStream,
            StreamResources.VoiceStream,
            StreamResources.AlarmStream,
            StreamResources.FMStream,
            StreamResources.MATVStream,
            StreamResources.MediaStream,
            StreamResources.NotificationStream,
            StreamResources.MasterStream,
            StreamResources.RemoteStream
    };

    /** Object that contains data for each slider */
    private class StreamControl {
        int streamType;
        ViewGroup group;
        ImageView icon;
        SeekBar seekbarView;
        int iconRes;
        int iconMuteRes;
    }

    // Synchronize when accessing this
    private ToneGenerator mToneGenerators[];
    private Vibrator mVibrator;

    private ContentObserver mSettingsObserver = new ContentObserver(this) {
        @Override
        public void onChange(boolean selfChange) {
            final int overlayStyle = Settings.System.getInt(mContext.getContentResolver(), PREF_MODE_VOLUME_OVERLAY, VOLUME_OVERLAY_SINGLE);
            changeOverlayStyle(overlayStyle);
        }
    };

    private static AlertDialog sConfirmSafeVolumeDialog;
    private static Object sConfirmSafeVolumeLock = new Object();

    private static class WarningDialogReceiver extends BroadcastReceiver
            implements DialogInterface.OnDismissListener {
        private Context mContext;
        private Dialog mDialog;

        WarningDialogReceiver(Context context, Dialog dialog) {
            mContext = context;
            mDialog = dialog;
            IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mDialog.cancel();
            synchronized (sConfirmSafeVolumeLock) {
                sConfirmSafeVolumeDialog = null;
            }
        }

        public void onDismiss(DialogInterface unused) {
            mContext.unregisterReceiver(this);
            synchronized (sConfirmSafeVolumeLock) {
                sConfirmSafeVolumeDialog = null;
            }
        }
    }

    public VolumePanel(final Context context, AudioService volumeService) {
        mContext = context;
        mAudioManager = (AudioManager2)context.getSystemService(Context.AUDIO_SERVICE);
        mAudioService = volumeService;

        // For now, only show master volume if master volume is supported
        boolean useMasterVolume = context.getResources().getBoolean(0x1110010);
        if (useMasterVolume) {
            for (int i = 0; i < STREAMS.length; i++) {
                StreamResources streamRes = STREAMS[i];
                streamRes.show = (streamRes.streamType == STREAM_MASTER);
            }
        }

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mView = inflater.inflate(0x10900e3, null);
        mView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                resetTimeout();
                return false;
            }
        });
        mPanel = (ViewGroup) mView.findViewById(0x10203ae);
        mSliderGroup = (ViewGroup) mView.findViewById(0x10203af);
        mMoreButton = (ImageView) mView.findViewById(0x1020323);
        mDivider = (ImageView) mView.findViewById(0x10203b0);

        mDialog = new Dialog(context, 0x103030a) {
            public boolean onTouchEvent(MotionEvent event) {
                if (isShowing() && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    forceTimeout();
                    return true;
                }
                return false;
            }
        };
        mDialog.setTitle("Volume control"); // No need to localize
        mDialog.setContentView(mView);
        mDialog.setOnDismissListener(new OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                mActiveStreamType = -1;
                mAudioManager.forceVolumeControlStream(mActiveStreamType);
            }
        });
        // Change some window properties
        Window window = mDialog.getWindow();
        window.setGravity(Gravity.TOP);
        LayoutParams lp = (LayoutParams)window.getAttributes();
        lp.token = null;
        // Offset from the top
        lp.y = mContext.getResources().getDimensionPixelOffset(0x105004d);
        lp.type = 2020;
        lp.width = LayoutParams.WRAP_CONTENT;
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.privateFlags |= 0x20;
        window.setAttributes(lp);
        window.addFlags(LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH);

        mToneGenerators = new ToneGenerator[AudioSystem.getNumStreamTypes()];
        mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        mVoiceCapable = context.getResources().getBoolean(0x1110030);
        mVolumeLinkNotification = true;     //MTK have linked this

        // Get the user's preferences
        final int chosenStyle = Settings.System.getInt(context.getContentResolver(), PREF_MODE_VOLUME_OVERLAY, VOLUME_OVERLAY_EXPANDABLE);
        changeOverlayStyle(chosenStyle);

        context.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(PREF_MODE_VOLUME_OVERLAY), false,
                mSettingsObserver);

        // This is new with 4.2 it seems
        boolean masterVolumeOnly = context.getResources().getBoolean(0x1110010);
        boolean masterVolumeKeySounds = mContext.getResources().getBoolean(0x1110011);

        mPlayMasterStreamTones = masterVolumeOnly && masterVolumeKeySounds;
        // End this is new

        mMoreButton.setOnClickListener(this);
        listenToRingerMode();
    }

    private void listenToRingerMode() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SKIN_CHANGED");
        mContext.registerReceiver(new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals("android.media.RINGER_MODE_CHANGED")) {
                    removeMessages(MSG_RINGER_MODE_CHANGED);
                    sendMessage(obtainMessage(MSG_RINGER_MODE_CHANGED));
                }
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    if (mDialog.isShowing()) {
                        forceTimeout();
                        return;
                    }
                } else
                if (action.equals("android.intent.action.SKIN_CHANGED")) {
                    createSliders();
                    reorderSliders(mActiveStreamType);
                    return;
                }
            }
        }, filter);
    }

    private void changeOverlayStyle(int newStyle) {
        Log.i("VolumePanel", "changeOverlayStyle : " + newStyle);
        // Don't change to the same style
        if (newStyle == mCurrentOverlayStyle) return;
        switch (newStyle) {
            case VOLUME_OVERLAY_SINGLE :
                mMoreButton.setVisibility(View.GONE);
                mDivider.setVisibility(View.GONE);
                mShowCombinedVolumes = false;
                mCurrentOverlayStyle = VOLUME_OVERLAY_SINGLE;
                break;
            case VOLUME_OVERLAY_EXPANDABLE :
                mMoreButton.setVisibility(View.VISIBLE);
                mDivider.setVisibility(View.VISIBLE);
                mShowCombinedVolumes = true;
                mCurrentOverlayStyle = VOLUME_OVERLAY_EXPANDABLE;
                break;
            case VOLUME_OVERLAY_EXPANDED :
                mMoreButton.setVisibility(View.GONE);
                mDivider.setVisibility(View.GONE);
                mShowCombinedVolumes = true;
                if (mCurrentOverlayStyle == VOLUME_OVERLAY_NONE) {
                    addOtherVolumes();
                    expand();
                }
                mCurrentOverlayStyle = VOLUME_OVERLAY_EXPANDED;
                break;
            case VOLUME_OVERLAY_NONE :
                mShowCombinedVolumes = false;
                mCurrentOverlayStyle = VOLUME_OVERLAY_NONE;
                break;
        }
    }

    private int getStreamMaxVolume(int streamType) {
        if (streamType == STREAM_MASTER) {
            return mAudioManager.getMasterMaxVolume();
        } else if (streamType == -200) {
            return mAudioService.getRemoteStreamMaxVolume();
        } else {
            return mAudioManager.getStreamMaxVolume(streamType);
        }
    }

    private int getStreamVolume(int streamType) {
        if (streamType == STREAM_MASTER) {
            return mAudioManager.getMasterVolume();
        } else if (streamType == -200) {
            return mAudioService.getRemoteStreamVolume();
        } else {
            return mAudioManager.getStreamVolume(streamType);
        }
    }

    private void setStreamVolume(int streamType, int index, int flags) {
        if (streamType == STREAM_MASTER) {
            mAudioManager.setMasterVolume(index, flags);
        } else if (streamType == -200) {
            mAudioService.setRemoteStreamVolume(index);
        } else {
            mAudioManager.setStreamVolume(streamType, index, flags);
        }
    }

    private boolean isMuted(int streamType) {
        if (streamType == STREAM_MASTER) {
            return mAudioManager.isMasterMute();
        } else if (streamType == -200) {
            return (mAudioService.getRemoteStreamVolume() <= 0);
        } else {
            return mAudioManager.isStreamMute(streamType);
        }
    }

    private void createSliders() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mStreamControls = new HashMap<Integer, StreamControl>(STREAMS.length);
        Resources res = mContext.getResources();
        for (int i = 0; i < STREAMS.length; i++) {
            StreamResources streamRes = STREAMS[i];
            int streamType = streamRes.streamType;
            StreamControl sc = new StreamControl();
            sc.streamType = streamType;
            sc.group = (ViewGroup) inflater.inflate(0x10900e4, null);
            sc.group.setTag(sc);
            sc.icon = (ImageView) sc.group.findViewById(0x10203b1);
            sc.icon.setTag(sc);
            sc.icon.setContentDescription(res.getString(streamRes.descRes));
            sc.iconRes = streamRes.iconRes;
            sc.iconMuteRes = streamRes.iconMuteRes;
            sc.icon.setImageResource(sc.iconRes);
            sc.icon.setOnClickListener(this);
            sc.seekbarView = (SeekBar) sc.group.findViewById(0x1020352);
            int plusOne = (streamType == 6 || streamType == 0) ? 1 : 0;
            sc.seekbarView.setMax(getStreamMaxVolume(streamType) + plusOne);
            sc.seekbarView.setOnSeekBarChangeListener(this);
            sc.seekbarView.setTag(sc);
            mStreamControls.put(streamType, sc);
        }
    }

    private void reorderSliders(int activeStreamType) {
        mSliderGroup.removeAllViews();

        StreamControl active = mStreamControls.get(activeStreamType);
        if (active == null) {
            Log.e("VolumePanel", "Missing stream type! - " + activeStreamType);
            mActiveStreamType = -1;
        } else {
            mSliderGroup.addView(active.group);
            mActiveStreamType = activeStreamType;
            active.group.setVisibility(View.VISIBLE);
            updateSlider(active);
        }

        addOtherVolumes();
    }

    private void addOtherVolumes() {
        if (!mShowCombinedVolumes) return;

        for (int i = 0; i < STREAMS.length; i++) {
            // Skip the phone specific ones and the active one
            final int streamType = STREAMS[i].streamType;
            if (!STREAMS[i].show || streamType == mActiveStreamType) {
                continue;
            }
            // Skip ring volume for non-phone devices
            if (!mVoiceCapable && streamType == 2) {
                continue;
            }

            // Skip notification volume if linked with ring volume
            if (mVoiceCapable && mVolumeLinkNotification && streamType == 5) {
                continue;
            }

            StreamControl sc = mStreamControls.get(streamType);
            mSliderGroup.addView(sc.group);
            updateSlider(sc);
        }
    }

    /** Update the mute and progress state of a slider */
    private void updateSlider(StreamControl sc) {
        sc.seekbarView.setProgress(getStreamVolume(sc.streamType));
        final boolean muted = isMuted(sc.streamType);
        sc.icon.setImageResource(muted ? sc.iconMuteRes : sc.iconRes);

        if (sc.streamType == 2 && mAudioManager.getRingerMode() == 1) {
            sc.icon.setImageResource(0x10802a1);
        }
        else if (sc.streamType == 3 && mAudioManager.getRingerMode() == 1) {
            sc.icon.setImageResource(0x10802a3);
        }
        if (sc.streamType == -200) {
            sc.seekbarView.setEnabled(true);
        } else if (sc.streamType != mAudioManager.getMasterStreamType() && muted) {
            sc.seekbarView.setEnabled(false);
        } else {
            sc.seekbarView.setEnabled(true);
        }
    }

    private boolean isExpanded() {
        return mMoreButton.getVisibility() != View.VISIBLE;
    }

    private void expand() {
        final int count = mSliderGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            if (mSliderGroup.getChildAt(i).getVisibility() != View.VISIBLE) {
                mSliderGroup.getChildAt(i).setVisibility(View.VISIBLE);
            }
        }
        mMoreButton.setVisibility(View.GONE);
        mDivider.setVisibility(View.GONE);
    }

    private void hideSlider(int mActiveStreamType) {
        final int count = mSliderGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            StreamControl sc = (StreamControl) mSliderGroup.getChildAt(i).getTag();
            if (mActiveStreamType == sc.streamType) {
                mSliderGroup.getChildAt(i).setVisibility(View.GONE);
            }
        }
    }

    private void collapse() {
        mMoreButton.setVisibility(View.VISIBLE);
        mDivider.setVisibility(View.VISIBLE);
        final int count = mSliderGroup.getChildCount();
        for (int i = 1; i < count; i++) {
            mSliderGroup.getChildAt(i).setVisibility(View.GONE);
        }
    }

    private void updateStates() {
        final int count = mSliderGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            StreamControl sc = (StreamControl) mSliderGroup.getChildAt(i).getTag();
            updateSlider(sc);
        }
    }

    public void postVolumeChanged(int streamType, int flags) {
        if (hasMessages(MSG_VOLUME_CHANGED)) return;
        synchronized (this) {
            if (mStreamControls == null) {
                createSliders();
            }
        }
        removeMessages(MSG_FREE_RESOURCES);
        obtainMessage(MSG_VOLUME_CHANGED, streamType, flags).sendToTarget();
    }

    public void postRemoteVolumeChanged(int streamType, int flags) {
        if (hasMessages(MSG_REMOTE_VOLUME_CHANGED)) return;
        synchronized (this) {
            if (mStreamControls == null) {
                createSliders();
            }
        }
        removeMessages(MSG_FREE_RESOURCES);
        obtainMessage(MSG_REMOTE_VOLUME_CHANGED, streamType, flags).sendToTarget();
    }

    public void postRemoteSliderVisibility(boolean visible) {
        obtainMessage(MSG_SLIDER_VISIBILITY_CHANGED, -200, visible ? 1 : 0).sendToTarget();
    }

    /**
     * Called by AudioService when it has received new remote playback information that
     * would affect the VolumePanel display (mainly volumes). The difference with
     * {@link #postRemoteVolumeChanged(int, int)} is that the handling of the posted message
     * (MSG_REMOTE_VOLUME_UPDATE_IF_SHOWN) will only update the volume slider if it is being
     * displayed.
     * This special code path is due to the fact that remote volume updates arrive to AudioService
     * asynchronously. So after AudioService has sent the volume update (which should be treated
     * as a request to update the volume), the application will likely set a new volume. If the UI
     * is still up, we need to refresh the display to show this new value.
     */
    public void postHasNewRemotePlaybackInfo() {
        if (hasMessages(MSG_REMOTE_VOLUME_UPDATE_IF_SHOWN)) return;
        // don't create or prevent resources to be freed, if they disappear, this update came too
        //   late and shouldn't warrant the panel to be displayed longer
        obtainMessage(MSG_REMOTE_VOLUME_UPDATE_IF_SHOWN).sendToTarget();
    }

    public void postMasterVolumeChanged(int flags) {
        postVolumeChanged(STREAM_MASTER, flags);
    }

    public void postMuteChanged(int streamType, int flags) {
        if (hasMessages(MSG_VOLUME_CHANGED)) return;
        synchronized (this) {
            if (mStreamControls == null) {
                createSliders();
            }
        }
        removeMessages(MSG_FREE_RESOURCES);
        obtainMessage(MSG_MUTE_CHANGED, streamType, flags).sendToTarget();
    }

    public void postMasterMuteChanged(int flags) {
        postMuteChanged(STREAM_MASTER, flags);
    }

    public void postDisplaySafeVolumeWarning() {
        if (hasMessages(MSG_DISPLAY_SAFE_VOLUME_WARNING)) return;
        obtainMessage(MSG_DISPLAY_SAFE_VOLUME_WARNING, 0, 0).sendToTarget();
    }

    /**
     * Override this if you have other work to do when the volume changes (for
     * example, vibrating, playing a sound, etc.). Make sure to call through to
     * the superclass implementation.
     */
    protected void onVolumeChanged(int streamType, int flags) {

        if (LOGD) Log.d(TAG, "onVolumeChanged(streamType: " + streamType + ", flags: " + flags + ")");

        if ((flags & 1) != 0) {
            synchronized (this) {
                if (streamType != mActiveStreamType) {
                    if (mCurrentOverlayStyle == VOLUME_OVERLAY_EXPANDABLE) {
                        hideSlider(mActiveStreamType);
                    }
                    reorderSliders(streamType);
                }
                onShowVolumeChanged(streamType, flags);
            }
        }

        if ((flags & 4) != 0 && ! mRingIsSilent) {
            removeMessages(MSG_PLAY_SOUND);
            sendMessageDelayed(obtainMessage(MSG_PLAY_SOUND, streamType, flags), PLAY_SOUND_DELAY);
        }

        if ((flags & 8) != 0) {
            removeMessages(MSG_PLAY_SOUND);
            removeMessages(MSG_VIBRATE);
            onStopSounds();
        }

        removeMessages(MSG_FREE_RESOURCES);
        sendMessageDelayed(obtainMessage(MSG_FREE_RESOURCES), FREE_DELAY);

        resetTimeout();
    }

    protected void onMuteChanged(int streamType, int flags) {

        if (LOGD) Log.d(TAG, "onMuteChanged(streamType: " + streamType + ", flags: " + flags + ")");

        StreamControl sc = mStreamControls.get(streamType);
        if (sc != null) {
            sc.icon.setImageResource(isMuted(sc.streamType) ? sc.iconMuteRes : sc.iconRes);
        }

        onVolumeChanged(streamType, flags);
    }

    protected void onShowVolumeChanged(int streamType, int flags) {
        int index = getStreamVolume(streamType);

        mRingIsSilent = false;

        if (LOGD) {
            Log.d(TAG, "onShowVolumeChanged(streamType: " + streamType
                    + ", flags: " + flags + "), index: " + index);
        }

        // get max volume for progress bar

        int max = getStreamMaxVolume(streamType);

        switch (streamType) {

            case 2: {
//                setRingerIcon();
                Uri ringuri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE);
                if (ringuri == null) {
                    mRingIsSilent = true;
                }
                break;
            }

            case 3: {
                // Special case for when Bluetooth is active for music
                if ((mAudioManager.getDevicesForStream(3) & 0x380) != 0) {
                    setMusicIcon(0x108029a, 0x108029b);
                } else {
                    setMusicIcon(0x10802a2, 0x10802a3);
                }
                break;
            }

            case 0: {
                /*
                 * For in-call voice call volume, there is no inaudible volume.
                 * Rescale the UI control so the progress bar doesn't go all
                 * the way to zero and don't show the mute icon.
                 */
                index++;
                max++;
                break;
            }

            case 4: {
                break;
            }

            case 5: {
                Uri ringuri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_NOTIFICATION);
                if (ringuri == null) {
                    mRingIsSilent = true;
                }
                break;
            }

            case 6: { //STREAM_BLUETOOTH_SCO
                index++;
                max++;
                break;
            }

            case -200: { //-200 RemoteStream
                if (LOGD) { Log.d(TAG, "showing remote volume "+index+" over "+ max); }
                break;
            }

            case 11: {  // MATV
                break;
            }

            case 10: {  // FM Stream
                StreamControl sc = (StreamControl)mStreamControls.get(10);
                if (sc == null)
                    break;
                sc.iconRes = 0x108036a;
                sc.iconMuteRes = 0x1080369;
                int vol = getStreamVolume(sc.streamType);
                ImageView icon = sc.icon;
                int cur_icon;
                if (vol == 0)
                    cur_icon = sc.iconMuteRes;
                else
                    cur_icon = sc.iconRes;
                icon.setImageResource(cur_icon);
                break;
            }
        }

        StreamControl sc = mStreamControls.get(streamType);
        if (sc != null) {
            if (sc.seekbarView.getMax() != max) {
                sc.seekbarView.setMax(max);
            }

            sc.seekbarView.setProgress(index);
            if (((flags & 0x20) != 0) || (streamType != mAudioManager.getMasterStreamType() && streamType != -200 && isMuted(streamType))) {
                sc.seekbarView.setEnabled(false);
            } else {
                sc.seekbarView.setEnabled(true);
            }
        }

        // Only Show if style needs it
        if (!mDialog.isShowing() && mCurrentOverlayStyle != VOLUME_OVERLAY_NONE) {
            int stream = (streamType == -200) ? -1 : streamType;
            // when the stream is for remote playback, use -1 to reset the stream type evaluation
            mAudioManager.forceVolumeControlStream(stream);
            mDialog.setContentView(mView);
            // Showing dialog - use collapsed state
            if (mShowCombinedVolumes && mCurrentOverlayStyle != VOLUME_OVERLAY_EXPANDED) {
                collapse();
            }
            // If just changed the style and we need to expand
            if (mCurrentOverlayStyle == VOLUME_OVERLAY_EXPANDED) {
                expand();
            }
            mDialog.show();
        }

        // Do a little vibrate if applicable (only when going into vibrate mode)
        if ((streamType != -200) && ((flags & 0x10) != 0) &&  mAudioService.isStreamAffectedByRingerMode(streamType) && mAudioManager.getRingerMode() == 1) {
            sendMessageDelayed(obtainMessage(MSG_VIBRATE), VIBRATE_DELAY);
        }
    }

    protected void onPlaySound(int streamType, int flags) {
        if (Settings.System.getInt(mContext.getContentResolver(), PREF_VOLUME_ADJUST_SOUNDS, 1) == 0) {
            return;
        }

        if (hasMessages(MSG_STOP_SOUNDS)) {
            removeMessages(MSG_STOP_SOUNDS);
            // Force stop right now
            onStopSounds();
        }

        synchronized (this) {
            ToneGenerator toneGen = getOrCreateToneGenerator(streamType);
            if (toneGen != null) {
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP);
                sendMessageDelayed(obtainMessage(MSG_STOP_SOUNDS), BEEP_DURATION);
            }
        }
    }

    protected void onStopSounds() {

        synchronized (this) {
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int i = numStreamTypes - 1; i >= 0; i--) {
                ToneGenerator toneGen = mToneGenerators[i];
                if (toneGen != null) {
                    toneGen.stopTone();
                }
            }
        }
    }

    protected void onVibrate() {

        // Make sure we ended up in vibrate ringer mode
        if (mAudioManager.getRingerMode() != 1) {
            return;
        }

        mVibrator.vibrate(VIBRATE_DURATION);
    }

    protected void onRemoteVolumeChanged(int streamType, int flags) {
        // streamType is the real stream type being affected, but for the UI sliders, we
        // refer to -200. We still play the beeps on the real
        // stream type.
        if (LOGD) Log.d(TAG, "onRemoteVolumeChanged(stream:"+streamType+", flags: " + flags + ")");

        if (((flags & 1) != 0) || mDialog.isShowing()) {
            synchronized (this) {
                if (mActiveStreamType != -200) {
                    reorderSliders(-200);
                }
                onShowVolumeChanged(-200, flags);
            }
        } else {
            if (LOGD) Log.d(TAG, "not calling onShowVolumeChanged(), no FLAG_SHOW_UI or no UI");
        }

        if ((flags & 4) != 0 && ! mRingIsSilent) {
            removeMessages(MSG_PLAY_SOUND);
            sendMessageDelayed(obtainMessage(MSG_PLAY_SOUND, streamType, flags), PLAY_SOUND_DELAY);
        }

        if ((flags & 8) != 0) {
            removeMessages(MSG_PLAY_SOUND);
            removeMessages(MSG_VIBRATE);
            onStopSounds();
        }

        removeMessages(MSG_FREE_RESOURCES);
        sendMessageDelayed(obtainMessage(MSG_FREE_RESOURCES), FREE_DELAY);

        resetTimeout();
    }

    protected void onRemoteVolumeUpdateIfShown() {
        if (LOGD) Log.d(TAG, "onRemoteVolumeUpdateIfShown()");
        if (mDialog.isShowing()
                && (mActiveStreamType == -200)
                && (mStreamControls != null)) {
            onShowVolumeChanged(-200, 0);
        }
    }


    /**
     * Handler for MSG_SLIDER_VISIBILITY_CHANGED
     * Hide or show a slider
     * @param streamType can be a valid stream type value, or VolumePanel.STREAM_MASTER,
     *                   or -200
     * @param visible
     */
    synchronized protected void onSliderVisibilityChanged(int streamType, int visible) {
        if (LOGD) Log.d(TAG, "onSliderVisibilityChanged(stream="+streamType+", visi="+visible+")");
        boolean isVisible = (visible == 1);
        for (int i = STREAMS.length - 1 ; i >= 0 ; i--) {
            StreamResources streamRes = STREAMS[i];
            if (streamRes.streamType == streamType) {
                streamRes.show = isVisible;
                if (!isVisible && (mActiveStreamType == streamType)) {
                    mActiveStreamType = -1;
                }
                break;
            }
        }
    }

    protected void onDisplaySafeVolumeWarning() {
        synchronized (sConfirmSafeVolumeLock) {
            if (sConfirmSafeVolumeDialog != null) {
                return;
            }
            sConfirmSafeVolumeDialog = new AlertDialog.Builder(mContext)
                    .setMessage(0x1040545)
                    .setPositiveButton(0x1040013,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    mAudioService.disableSafeMediaVolume();
                                }
                            })
                    .setNegativeButton(0x1040009, null)
                    .setIconAttribute(0x1010355)
                    .create();
            final WarningDialogReceiver warning = new WarningDialogReceiver(mContext,
                    sConfirmSafeVolumeDialog);

            sConfirmSafeVolumeDialog.setOnDismissListener(warning);
            sConfirmSafeVolumeDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            sConfirmSafeVolumeDialog.show();
        }
    }

    /**
     * Lock on this VolumePanel instance as long as you use the returned ToneGenerator.
     */
    private ToneGenerator getOrCreateToneGenerator(int streamType) {
        if (streamType == STREAM_MASTER) {
            // For devices that use the master volume setting only but still want to
            // play a volume-changed tone, direct the master volume pseudostream to
            // the system stream's tone generator.
            if (mPlayMasterStreamTones) {
                streamType = 1;
            } else {
                return null;
            }
        }
        synchronized (this) {
            if (mToneGenerators[streamType] == null) {
                try {
                    mToneGenerators[streamType] = new ToneGenerator(streamType, MAX_VOLUME);
                } catch (RuntimeException e) {
                    if (LOGD) {
                        Log.d(TAG, "ToneGenerator constructor failed with "
                                + "RuntimeException: " + e);
                    }
                }
            }
            return mToneGenerators[streamType];
        }
    }


    /**
     * Switch between icons because Bluetooth music is same as music volume, but with
     * different icons.
     */
    private void setMusicIcon(int resId, int resMuteId) {
        StreamControl sc = mStreamControls.get(3);
        if (sc != null) {
            sc.iconRes = resId;
            sc.iconMuteRes = resMuteId;
            sc.icon.setImageResource(isMuted(sc.streamType) ? sc.iconMuteRes : sc.iconRes);
        }
    }

    protected void onFreeResources() {
        synchronized (this) {
            for (int i = mToneGenerators.length - 1; i >= 0; i--) {
                if (mToneGenerators[i] != null) {
                    mToneGenerators[i].release();
                }
                mToneGenerators[i] = null;
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {

            case MSG_VOLUME_CHANGED: {
                onVolumeChanged(msg.arg1, msg.arg2);
                break;
            }

            case MSG_MUTE_CHANGED: {
                onMuteChanged(msg.arg1, msg.arg2);
                break;
            }

            case MSG_FREE_RESOURCES: {
                onFreeResources();
                break;
            }

            case MSG_STOP_SOUNDS: {
                onStopSounds();
                break;
            }

            case MSG_PLAY_SOUND: {
                onPlaySound(msg.arg1, msg.arg2);
                break;
            }

            case MSG_VIBRATE: {
                onVibrate();
                break;
            }

            case MSG_TIMEOUT: {
                if (mDialog.isShowing()) {
                    mDialog.dismiss();
                    mActiveStreamType = -1;
                }
                break;
            }
            case MSG_RINGER_MODE_CHANGED: {
                if (mDialog.isShowing()) {
                    updateStates();
                }
                break;
            }

            case MSG_REMOTE_VOLUME_CHANGED: {
                onRemoteVolumeChanged(msg.arg1, msg.arg2);
                break;
            }

            case MSG_REMOTE_VOLUME_UPDATE_IF_SHOWN:
                onRemoteVolumeUpdateIfShown();
                break;

            case MSG_SLIDER_VISIBILITY_CHANGED:
                onSliderVisibilityChanged(msg.arg1, msg.arg2);
                break;

            case MSG_DISPLAY_SAFE_VOLUME_WARNING:
                onDisplaySafeVolumeWarning();
                break;
        }
    }

    private void resetTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessageDelayed(obtainMessage(MSG_TIMEOUT), TIMEOUT_DELAY);
    }

    private void forceTimeout() {
        removeMessages(MSG_TIMEOUT);
        sendMessage(obtainMessage(MSG_TIMEOUT));
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        final Object tag = seekBar.getTag();
        if (fromUser && tag instanceof StreamControl) {
            StreamControl sc = (StreamControl) tag;
            if (getStreamVolume(sc.streamType) != progress) {
                setStreamVolume(sc.streamType, progress, 0);
            }
        }
        resetTimeout();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        final Object tag = seekBar.getTag();
        if (tag instanceof StreamControl) {
            StreamControl sc = (StreamControl) tag;
            // because remote volume updates are asynchronous, AudioService might have received
            // a new remote volume value since the finger adjusted the slider. So when the
            // progress of the slider isn't being tracked anymore, adjust the slider to the last
            // "published" remote volume value, so the UI reflects the actual volume.
            if (sc.streamType == -200) {
                seekBar.setProgress(getStreamVolume(-200));
            }
        }
    }

    public void onClick(View v) {
        if (v == mMoreButton) {
            expand();
        } else if (v instanceof ImageView) {
            Intent volumeSettings = new Intent(android.provider.Settings.ACTION_SOUND_SETTINGS);
            volumeSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            forceTimeout();
            mContext.startActivity(volumeSettings);
            return;
        }
        resetTimeout();
    }
}
