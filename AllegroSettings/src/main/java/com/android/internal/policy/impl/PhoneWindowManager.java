package com.android.internal.policy.impl;
import android.view.KeyEvent;
import android.os.Handler;
import android.os.Message;
import android.view.ViewConfiguration;
import android.provider.Settings;
import android.content.Context;
import android.view.HapticFeedbackConstants;
import android.content.ContentResolver;
import android.widget.Toast;



public class PhoneWindowManager {
    Handler mHandler;
    Context mContext;
    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_DISPATCH_VOLKEY_WITH_WAKE_LOCK = 5;
    int START_ADD;
    boolean mVolBtnMusicControls;
    boolean mIsLongPress;

    void handleVolumeKey(int stream, int keycode) { }
    private void enablePointerLocation() { }
    private void disablePointerLocation() { }
    void dispatchMediaKeyWithWakeLock(KeyEvent event) { }
    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent event) { }
    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent event) {}

    private class PolicyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ENABLE_POINTER_LOCATION:
                    enablePointerLocation();
                    break;
                case MSG_DISABLE_POINTER_LOCATION:
                    disablePointerLocation();
                    break;
                case MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK:
                    dispatchMediaKeyWithWakeLock((KeyEvent)msg.obj);
                    break;
                case MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
                    dispatchMediaKeyRepeatWithWakeLock((KeyEvent)msg.obj);
                    break;
                case MSG_DISPATCH_VOLKEY_WITH_WAKE_LOCK:
                    mIsLongPress = true;
                    dispatchMediaKeyWithWakeLockToAudioService((KeyEvent)msg.obj);
                    dispatchMediaKeyWithWakeLockToAudioService(KeyEvent.changeAction((KeyEvent)msg.obj, KeyEvent.ACTION_UP));
                    break;
            }
        }
    }

    public boolean performHapticFeedbackLw(Object win, int effectId, boolean always) { return true; }
    
    Runnable mBackLongPress = new Runnable() {
        public void run() {
            if (killForegroundApplication(mContext)) {
                performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false);
                Toast.makeText(mContext, 0x1040559, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public long interceptKeyBeforeDispatching_Custom(KeyEvent event)
    {
        final int keyCode = event.getKeyCode();
        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final int repeatCount = event.getRepeatCount();

        if (keyCode == KeyEvent.KEYCODE_BACK && !down) {
            mHandler.removeCallbacks(mBackLongPress);
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (Settings.System.getInt(mContext.getContentResolver(), "kill_app_longpress_back", 0) == 1) {
                if (down && repeatCount == 0) {
                    mHandler.postDelayed(mBackLongPress, 500);
                }
            }
        }

        return 0;
    }

    public long interceptKeyBeforeDispatching(Object win, KeyEvent event, int policyFlags) {
        interceptKeyBeforeDispatching_Custom(event);
        return 1;
    }

    public static boolean killForegroundApplication(Context context) {
        boolean targetKilled = false;
        return targetKilled;
    }

    public void updateCustomSettings(ContentResolver resolver) {
        mVolBtnMusicControls = (Settings.System.getInt(resolver, "volbtn_music_controls", 1) == 1);
    }

    public void HandleMusicVol(KeyEvent event, boolean isScreenOn)
    {
        int keyCode = event.getKeyCode();
        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;

        if (mVolBtnMusicControls && down && (keyCode != KeyEvent.KEYCODE_VOLUME_MUTE)) {
            mIsLongPress = false;
            int newKeyCode = event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ? KeyEvent.KEYCODE_MEDIA_NEXT : KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            Message msg = mHandler.obtainMessage(5, new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), newKeyCode, 0));
            //msg.setAsynchronous(true);
            mHandler.sendMessageDelayed(msg, ViewConfiguration.getLongPressTimeout());
            return;
        } else {
            if (mVolBtnMusicControls && !down) {
                mHandler.removeMessages(5);
                if (mIsLongPress) {
                    return;
                }
            }
            if (!isScreenOn) {
                handleVolumeKey(3, keyCode);
            }
        }
    }
}
