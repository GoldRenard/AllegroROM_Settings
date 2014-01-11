package com.goldrenard.allegrosettings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.goldrenard.allegrosettings.SeekBarDialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;

public class TransparencyPreference extends SeekBarDialogPreference
        implements android.widget.SeekBar.OnSeekBarChangeListener
{

    private static final int SEEK_BAR_RANGE = 255;
    private static final String sName = "status_bar_transparency";
    private int mCurrentTransparency;
    private SeekBar mSeekBar;

    public TransparencyPreference(Context context, AttributeSet attributeset)
    {
        super(context, attributeset);
        setDialogLayoutResource(R.layout.pref_dialog_statusbar);
        setDialogIcon(R.drawable.ic_settings_themes);
        mCurrentTransparency = android.provider.Settings.System.getInt(getContext().getContentResolver(), sName, SEEK_BAR_RANGE);
    }

    private void SetTransparency(int value)
    {
        Intent intent = new Intent("AllegroSettings.intent.action.TRANSPARENCY_CHANGED");
        intent.putExtra("value", value);
        Log.d("AllegroSettings", "Sending transparent intent with value " + value);
        getContext().sendBroadcast(intent);
    }

    protected void onBindDialogView(View view)
    {
        super.onBindDialogView(view);
        mSeekBar = getSeekBar(view);
        mSeekBar.setMax(SEEK_BAR_RANGE);
        mSeekBar.setEnabled(true);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setProgress(SEEK_BAR_RANGE - mCurrentTransparency);
    }

    protected void onDialogClosed(boolean isClosed)
    {
        super.onDialogClosed(isClosed);
        if (isClosed)
        {
            mCurrentTransparency = SEEK_BAR_RANGE - mSeekBar.getProgress();
            android.provider.Settings.System.putInt(getContext().getContentResolver(), sName, mCurrentTransparency);
        }
        SetTransparency(mCurrentTransparency);
    }

    public void onProgressChanged(SeekBar seekbar, int i, boolean flag)
    {
    }

    public void onStartTrackingTouch(SeekBar seekbar)
    {
    }

    public void onStopTrackingTouch(SeekBar seekbar)
    {
    }

    protected void showDialog(Bundle bundle)
    {
        super.showDialog(bundle);
    }
}