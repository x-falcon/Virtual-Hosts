/*
 **Copyright (C) 2017  xfalcon
 **
 **This program is free software: you can redistribute it and/or modify
 **it under the terms of the GNU General Public License as published by
 **the Free Software Foundation, either version 3 of the License, or
 **(at your option) any later version.
 **
 **This program is distributed in the hope that it will be useful,
 **but WITHOUT ANY WARRANTY; without even the implied warranty of
 **MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 **GNU General Public License for more details.
 **
 **You should have received a copy of the GNU General Public License
 **along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **
 */

package com.github.xfalcon.vhosts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.preference.*;
import com.github.xfalcon.vhosts.util.FileUtils;
import com.github.xfalcon.vhosts.util.HttpUtils;
import com.github.xfalcon.vhosts.util.LogUtils;
import com.github.xfalcon.vhosts.vservice.DnsChange;

public class SettingsFragment extends PreferenceFragmentCompat implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static String TAG = SettingsFragment.class.getName();

    public static final int VPN_REQUEST_CODE = 0x0F;
    public static final int SELECT_FILE_CODE = 0x05;
    public static final String PREFS_NAME = SettingsFragment.class.getName();
    public static final String IS_NET = "IS_NET";
    public static final String HOSTS_URL = "HOSTS_URL";
    public static final String HOSTS_URI = "HOST_URI";
    public static final String NET_HOST_FILE = "net_hosts";
    public static final String IPV4_DNS = "IPV4_DNS";
    public static final String IS_CUS_DNS= "IS_CUS_DNS";

    private Handler handler=null;


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        final SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        PreferenceScreen prefScreen = getPreferenceScreen();
        handeleSummary(prefScreen, sharedPreferences);
        Preference yourCustomPref = (Preference) findPreference(HOSTS_URL);
        yourCustomPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            public boolean onPreferenceClick(Preference preference) {
                String url = sharedPreferences.getString(HOSTS_URL,"");
                setProgressDialog(preference.getContext(),url);

                return true;
            }
        });
    }
    public void setProgressDialog(final Context context, final String url) {

        int llPadding = 30;
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(context);
        tvText.setText("Download ...");
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setView(ll);

        final AlertDialog dialog = builder.create();
        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
        handler=new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Looper.prepare();
                    String result = HttpUtils.get(url);
                    FileUtils.writeFile(context.openFileOutput(NET_HOST_FILE, Context.MODE_PRIVATE), result);
                    Toast.makeText(context, String.format(getString(R.string.down_success), DnsChange.handle_hosts(context.openFileInput(NET_HOST_FILE))), Toast.LENGTH_LONG).show();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            dialog.hide();
                        }
                    });
                    Looper.loop();
                } catch (Exception e) {
                    Toast.makeText(context, getString(R.string.down_error), Toast.LENGTH_LONG).show();
                    LogUtils.e(TAG, e.getMessage(), e);
                }

            }
        }).start();
        dialog.show();

    }

    private void handeleSummary(PreferenceGroup preferenceGroup, SharedPreferences sharedPreferences) {
        int count = preferenceGroup.getPreferenceCount();

        for (int i = 0; i < count; i++) {
            Preference p = preferenceGroup.getPreference(i);
            if (p instanceof PreferenceCategory) {
                handeleSummary((PreferenceCategory) p, sharedPreferences);
            }
            if (!(p instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(p.getKey(), "");
                setPreferenceSummary(p, value);
            }
        }
    }

    private void setPreferenceSummary(Preference preference, String value) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(value);
            if (prefIndex >= 0) {
                listPreference.setSummary(listPreference.getEntries()[prefIndex]);
            }
        } else if (preference instanceof EditTextPreference) {
            preference.setSummary(value);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Preference preference = findPreference(key);
        if (null != preference) {
            if (!(preference instanceof CheckBoxPreference)) {
                String value = sharedPreferences.getString(preference.getKey(), "");
                setPreferenceSummary(preference, value);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}
