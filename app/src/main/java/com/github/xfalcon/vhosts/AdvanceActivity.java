package com.github.xfalcon.vhosts;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import com.github.xfalcon.vhosts.util.FileUtils;
import com.github.xfalcon.vhosts.util.HttpUtils;
import com.github.xfalcon.vhosts.util.LogUtils;
import com.github.xfalcon.vhosts.vservice.DnsChange;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.xfalcon.vhosts.VhostsActivity.IS_LOCAL;
import static com.github.xfalcon.vhosts.VhostsActivity.PREFS_NAME;

public class AdvanceActivity extends AppCompatActivity {
    private static final String TAG = AdvanceActivity.class.getSimpleName();
    private Handler handler=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advance);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        final Button confirm_button = findViewById(R.id.confirm_button);
        final RadioButton local_radio_button = findViewById(R.id.local_radio_button);
        final RadioButton net_radio_button = findViewById(R.id.net_radio_button);
        final EditText url_edit_text = findViewById(R.id.url_edit_text);
        final RadioGroup ln_radio_group = findViewById(R.id.ln_radio_group);
        final ImageButton down_button = findViewById(R.id.down_button);
        final ProgressBar progressBar = findViewById(R.id.progressBar);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        url_edit_text.setText(settings.getString(VhostsActivity.HOSTS_URL,"https://raw.githubusercontent.com/x-falcon/tools/master/hosts"));
        url_edit_text.setSelection(4);
        boolean isLocal = settings.getBoolean(IS_LOCAL, true);
        if (isLocal) local_radio_button.setChecked(true);
        else net_radio_button.setChecked(true);
        handler=new Handler();

        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        down_button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                try {
                    down_button.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            try {
                                String result = HttpUtils.get(url_edit_text.getText().toString());
                                FileUtils.writeFile(openFileOutput(VhostsActivity.NET_HOST_FILE, Context.MODE_PRIVATE), result);
                                Toast.makeText(getApplication(), String.format(getString(R.string.down_success), DnsChange.handle_hosts(openFileInput(VhostsActivity.NET_HOST_FILE))), Toast.LENGTH_LONG).show();
                                SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(VhostsActivity.HOSTS_URL,url_edit_text.getText().toString());
                                editor.apply();
                            } catch (Exception e) {
                                Toast.makeText(getApplication(), getString(R.string.down_error), Toast.LENGTH_LONG).show();
                                LogUtils.e(TAG, e.getMessage(), e);
                            }
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    down_button.setEnabled(true);
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                            Looper.loop();

                        }
                    }).start();

                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }
            }
        });

        url_edit_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isUrl(editable.toString())) {
                    url_edit_text.setError(null);
                    down_button.setEnabled(true);
                } else {
                    url_edit_text.setError(getString(R.string.url_error));
                    down_button.setEnabled(false);
                }
            }
        });

        ln_radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                if (checkedId == R.id.local_radio_button) editor.putBoolean(IS_LOCAL, true);
                else editor.putBoolean(IS_LOCAL, false);
                editor.apply();
            }
        });


    }

    public boolean isUrl(String str) {
        String regex = "http(s)?://([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?";
        return this.match(regex, str);
    }

    private boolean match(String regex, String str) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
