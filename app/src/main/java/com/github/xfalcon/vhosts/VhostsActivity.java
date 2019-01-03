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

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.baidu.mobstat.StatService;
import com.github.clans.fab.FloatingActionButton;
import com.github.xfalcon.vhosts.util.LogUtils;
import com.github.xfalcon.vhosts.vservice.VhostsService;
import com.suke.widget.SwitchButton;

import java.lang.reflect.Field;

public class VhostsActivity extends AppCompatActivity {

    private static final String TAG = VhostsActivity.class.getSimpleName();
    private static final int VPN_REQUEST_CODE = 0x0F;
    private static final int SELECT_FILE_CODE = 0x05;
    public static final String PREFS_NAME = VhostsActivity.class.getName();
    public static final String IS_LOCAL = "IS_LOCAL";
    public static final String HOSTS_URL = "HOSTS_URL";
    public static final String HOSTS_URI = "HOST_URI";
    public static final String NET_HOST_FILE = "net_hosts";


    private boolean waitingForVPNStart;

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (VhostsService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
                if (intent.getBooleanExtra("running", false))
                    waitingForVPNStart = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launch();
        StatService.autoTrace(this, true, false);
        setContentView(R.layout.activity_vhosts);
        LogUtils.context = getApplicationContext();
        final SwitchButton vpnButton = findViewById(R.id.button_start_vpn);

        final Button selectHosts = findViewById(R.id.button_select_hosts);
        final FloatingActionButton fab_boot = findViewById(R.id.fab_boot);
        final FloatingActionButton fab_donation = findViewById(R.id.fab_donation);
        if (checkHostUri() == -1) {
            selectHosts.setText(getString(R.string.select_hosts));
        }
        if (BootReceiver.getEnabled(this)) {
            fab_boot.setColorNormalResId(R.color.startup_on);
        }
        vpnButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    if (checkHostUri() == -1) {
                        showDialog();
                    } else {
                        startVPN();
                    }
                } else {
                    shutdownVPN();
                }
            }
        });

        fab_boot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (BootReceiver.getEnabled(v.getContext())) {
                    BootReceiver.setEnabled(v.getContext(), false);
                    fab_boot.setColorNormalResId(R.color.startup_off);
                } else {
                    BootReceiver.setEnabled(v.getContext(), true);
                    fab_boot.setColorNormalResId(R.color.startup_on);
                }
            }
        });
        selectHosts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        });
        selectHosts.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                startActivity(new Intent(getApplicationContext(), AdvanceActivity.class));

                return false;
            }
        });
        fab_donation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), DonationActivity.class));
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(VhostsService.BROADCAST_VPN_STATE));
    }

    private void launch() {
        Uri uri = getIntent().getData();
        if (uri == null) return;
        String data_str = uri.toString();
        if ("on".equals(data_str)) {
            if (!VhostsService.isRunning())
                VhostsService.startVService(this,1);
            finish();
        } else if ("off".equals(data_str)) {
            VhostsService.stopVService(this);
            finish();
        }
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        try {
            String SHOW_ADVANCED;
            try {
                Field f = android.provider.DocumentsContract.class.getField("EXTRA_SHOW_ADVANCED");
                SHOW_ADVANCED = f.get(f.getName()).toString();
            }catch (NoSuchFieldException e){
                LogUtils.e(TAG,e.getMessage(),e);
                SHOW_ADVANCED = "android.content.extra.SHOW_ADVANCED";
            }
            intent.putExtra(SHOW_ADVANCED, true);
        } catch (Throwable e) {
            LogUtils.e(TAG, "SET EXTRA_SHOW_ADVANCED", e);
        }

        try {
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, SELECT_FILE_CODE);
        } catch (Exception e) {
            Toast.makeText(this, R.string.file_select_error, Toast.LENGTH_LONG).show();
            LogUtils.e(TAG, "START SELECT_FILE_ACTIVE FAIL",e);
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(IS_LOCAL, false);
            editor.apply();
            startActivity(new Intent(getApplicationContext(), AdvanceActivity.class));
        }

    }

    private void startVPN() {
        waitingForVPNStart = false;
        Intent vpnIntent = VhostsService.prepare(this);
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    private int checkHostUri() {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (settings.getBoolean(VhostsActivity.IS_LOCAL, true)) {
            try {
                getContentResolver().openInputStream(Uri.parse(settings.getString(HOSTS_URI, null))).close();
                return 1;
            } catch (Exception e) {
                LogUtils.e(TAG, "HOSTS FILE NOT FOUND", e);
                return -1;
            }
        } else {
            try {
                openFileInput(VhostsActivity.NET_HOST_FILE).close();
                return 2;
            } catch (Exception e) {
                LogUtils.e(TAG, "NET HOSTS FILE NOT FOUND", e);
                return -2;
            }
        }
    }

    private void setUriByPREFS(Intent intent) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        Uri uri = intent.getData();
        final int takeFlags = intent.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        try {
            getContentResolver().takePersistableUriPermission(uri, takeFlags);
            editor.putString(HOSTS_URI, uri.toString());
            editor.apply();
            if (checkHostUri() == 1) {
                setButton(true);
                setButton(false);
            } else {
                Toast.makeText(this, R.string.permission_error, Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            LogUtils.e(TAG, "permission error", e);
        }

    }

    private void shutdownVPN() {
        if (VhostsService.isRunning())
            startService(new Intent(this, VhostsService.class).setAction(VhostsService.ACTION_DISCONNECT));
        setButton(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            waitingForVPNStart = true;
            startService(new Intent(this, VhostsService.class).setAction(VhostsService.ACTION_CONNECT));
            setButton(false);
        } else if (requestCode == SELECT_FILE_CODE && resultCode == RESULT_OK) {
            setUriByPREFS(data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setButton(!waitingForVPNStart && !VhostsService.isRunning());
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void setButton(boolean enable) {
        final SwitchButton vpnButton = (SwitchButton) findViewById(R.id.button_start_vpn);
        final Button selectHosts = (Button) findViewById(R.id.button_select_hosts);
        if (enable) {
            vpnButton.setChecked(false);
            selectHosts.setAlpha(1.0f);
            selectHosts.setClickable(true);
        } else {
            vpnButton.setChecked(true);
            selectHosts.setAlpha(.5f);
            selectHosts.setClickable(false);
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_title);
        builder.setMessage(R.string.dialog_message);
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                selectFile();
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setButton(true);
            }
        });
        builder.show();
    }

}
