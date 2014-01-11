package com.android.settings;

import com.android.settings.accounts.AuthenticatorHelper;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Settings extends PreferenceActivity {

    protected HashMap<Integer, Integer> mHeaderIndexMap = new HashMap<Integer, Integer>();
    private Header mFirstHeader;

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static final int HEADER_TYPE_CATEGORY = 0;
        public HeaderAdapter(Context context, List<Header> objects, AuthenticatorHelper authenticatorHelper) { super(context, 0, objects); }
        static int getHeaderType(Header header) { return HEADER_TYPE_CATEGORY; }
    }

    private void updateHeaderList_Custom(List<Header> target, Header header) {
            // Ids are integers, so downcasting
            int id = (int) header.id;
            if (id == 0x7f080277) {
                Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
                launcherIntent.addCategory(Intent.CATEGORY_HOME);
                launcherIntent.addCategory(Intent.CATEGORY_DEFAULT);

                Intent launcherPreferencesIntent = new Intent(Intent.ACTION_MAIN);
                launcherPreferencesIntent.addCategory("com.cyanogenmod.category.LAUNCHER_PREFERENCES");

                ActivityInfo defaultLauncher = getPackageManager().resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo;
                launcherPreferencesIntent.setPackage(defaultLauncher.packageName);
                ResolveInfo launcherPreferences = getPackageManager().resolveActivity(launcherPreferencesIntent, 0);
                if (launcherPreferences != null) {
                    header.intent = new Intent().setClassName(launcherPreferences.activityInfo.packageName, launcherPreferences.activityInfo.name);
                } else {
                    target.remove(header);
                }
            }
    }

    private void updateHeaderList(List<Header> target) {

        int i = 0;
        mHeaderIndexMap.clear();
        while (i < target.size()) {
            Header header = target.get(i);
            updateHeaderList_Custom(target, header);
        }
    }

}
