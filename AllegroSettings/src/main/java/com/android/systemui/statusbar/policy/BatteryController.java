package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.Iterator;
import java.util.ArrayList;

public class BatteryController extends BroadcastReceiver {
    private static final String TAG = "StatusBar.BatteryController";
    private static final String STATUS_BAR_BATTERY_PREF = "status_bar_battery";

    private Context mContext;

    private ArrayList<ImageView> mIconViews = new ArrayList<ImageView>();
    private ArrayList<TextView> mLabelViews = new ArrayList<TextView>();

    public static final int BATTERY_STYLE_NORMAL         = 0;
    public static final int BATTERY_STYLE_PERCENT        = 1;
    /***
     * BATTERY_STYLE_CIRCLE* cannot be handled in this controller, since we cannot get views from
     * statusbar here. Yet it is listed for completion and not to confuse at future updates
     * See CircleBattery.java for more info
     *
     * set to public to be reused by CircleBattery
     */
    public static final int BATTERY_STYLE_CIRCLE         = 2;
    public static final int BATTERY_STYLE_CIRCLE_PERCENT = 3;
    public static final int BATTERY_STYLE_GONE           = 4;

    private static final int BATTERY_TEXT_STYLE_NORMAL  = 0x7f0b005b;
    private static final int BATTERY_TEXT_STYLE_MIN     = 0x7f0b00ef;

    private boolean mBatteryPlugged = false;
    private int mBatteryStatus = BatteryManager.BATTERY_STATUS_UNKNOWN;
    private int mBatteryLevel = 0;
    private int mBatteryStyle;

    Handler mHandler;
    private boolean mUiController = false;

    private static final String ACTION_BATTERY_PERCENTAGE_SWITCH = "mediatek.intent.action.BATTERY_PERCENTAGE_SWITCH";
    private String mBatteryPercentage;
    private boolean mBatteryProtection;
    private boolean mShouldShowBatteryPercentage;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(STATUS_BAR_BATTERY_PREF), false, this);
        }

        @Override public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    private ArrayList<BatteryStateChangeCallback> mChangeCallbacks =
            new ArrayList<BatteryStateChangeCallback>();

    public interface BatteryStateChangeCallback {
        public void onBatteryLevelChanged(int level, int status);
    }

    public BatteryController(Context context) {
        this(context, true);
    }

    public BatteryController(Context context, boolean ui) {
        mContext = context;
        mHandler = new Handler();
        mUiController = ui;

        if (mUiController) {
            SettingsObserver settingsObserver = new SettingsObserver(mHandler);
            settingsObserver.observe();
            updateSettings();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(this, filter);
    }

    public void addIconView(ImageView v){
        mIconViews.add(v);
    }

    public void addLabelView(TextView v){
        mLabelViews.add(v);
    }

    public void addStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
        // trigger initial update
        cb.onBatteryLevelChanged(getBatteryLevel(), getBatteryStatus());
    }

    public void removeStateChangedCallback(BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    // Allow override battery icons
    public int getIconStyleUnknown() {
        return 0x7f02013c;
    }
    public int getIconStyleNormal() {
        return 0x7f02013c;
    }
    public int getIconStyleCharge() {
        return 0x7f020145;
    }
    public int getIconStyleNormalMin() {
        return 0x7f020260;
    }
    public int getIconStyleChargeMin() {
        return 0x7f02025f;
    }

    protected int getBatteryLevel() {
        return mBatteryLevel;
    }

    protected int getBatteryStyle() {
        return mBatteryStyle;
    }

    protected int getBatteryStatus() {
        return mBatteryStatus;
    }

    protected boolean isBatteryPlugged() {
        return mBatteryPlugged;
    }

    protected boolean isBatteryPresent() {
        // the battery widget always is shown.
        return true;
    }

    protected boolean isBatteryStatusUnknown() {
        return getBatteryStatus() == BatteryManager.BATTERY_STATUS_UNKNOWN;
    }

    protected boolean isBatteryStatusCharging() {
        return getBatteryStatus() == BatteryManager.BATTERY_STATUS_CHARGING;
    }

    protected boolean isUiController() {
        return mUiController;
    }

    public void onReceive(Context context, Intent intent)
    {
        final String action = intent.getAction();

        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            mBatteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            updateViews();
            if (mUiController) {
                updateBattery();
            }
        }
    }

    protected void updateViews() {
        int level = getBatteryLevel();
        if (mUiController) {
            int N = mIconViews.size();
            for (int i=0; i<N; i++) {
                ImageView v = mIconViews.get(i);
                v.setImageLevel(level);
                v.setContentDescription(mContext.getString(0x7f0b009d, level));
            }
            N = mLabelViews.size();
            for (int i=0; i<N; i++) {
                TextView v = mLabelViews.get(i);
                v.setText(mContext.getString(BATTERY_TEXT_STYLE_MIN, level));
            }
        }

        for (BatteryStateChangeCallback cb : mChangeCallbacks) {
            cb.onBatteryLevelChanged(level, getBatteryStatus());
        }
    }

    protected void updateBattery() {
        int mIcon = View.GONE;
        int mText = View.GONE;
        int mIconStyle = getIconStyleNormal();

        if (isBatteryPresent()) {
            if ( isBatteryStatusUnknown() && (mBatteryStyle == BATTERY_STYLE_NORMAL || mBatteryStyle == BATTERY_STYLE_PERCENT)) {
                // Unknown status doesn't relies on any style
                mIcon = (View.VISIBLE);
                mIconStyle = getIconStyleUnknown();
            } else if (mBatteryStyle == BATTERY_STYLE_NORMAL) {
                mIcon = (View.VISIBLE);
                mIconStyle = isBatteryStatusCharging() ? getIconStyleCharge() : getIconStyleNormal();
            } else if (mBatteryStyle == BATTERY_STYLE_PERCENT) {
                mIcon = (View.VISIBLE);
                mText = (View.VISIBLE);
                mIconStyle = isBatteryStatusCharging() ? getIconStyleChargeMin() : getIconStyleNormalMin();
            }
        }

        int N = mIconViews.size();
        for (int i=0; i<N; i++) {
            ImageView v = mIconViews.get(i);
            v.setVisibility(mIcon);
            v.setImageResource(mIconStyle);
        }
        N = mLabelViews.size();
        for (int i=0; i<N; i++) {
            TextView v = mLabelViews.get(i);
            v.setVisibility(mText);
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        mBatteryStyle = (Settings.System.getInt(resolver, STATUS_BAR_BATTERY_PREF, BATTERY_STYLE_NORMAL));
        updateBattery();
    }
}
