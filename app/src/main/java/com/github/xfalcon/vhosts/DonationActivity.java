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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.github.xfalcon.vhosts.util.*;

public class DonationActivity extends AppCompatActivity {

    private final String TAG = DonationActivity.class.getSimpleName();
    /**
     * Google
     */
    private static final boolean IS_GooglePlay=BuildConfig.IS_GooglePlay;
    private static final int PLAY_PURCHASE_REQUEST_CODE = 0x15;
    private static final String STORE_URI2 ="https://play.google.com/store/apps/details?id=com.github.xfalcon.vhosts";
    private static final String STORE_URI ="market://details?id=com.github.xfalcon.vhosts";
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxic/nGZvXgn3O0Sq8kSZCMZ0g6I1VYm1AoT8FlXegHJ1Fm+PAZ0Na0NLEXnP7okdxlRuUsO9SmUH4lv0Mz3sY0RXwIPCchiCMUn8wKGXtKY4Bh+R3xdt8DLDBc+h4i9jZYnVW1ntv4yuQrcowrUHQauRW0+sv+AGQmJuJdASCqwLgMBGa4uZh0uDmtTTfhEFaHmaUHlRBA4EgrABIJCF5QDQyYYDx75pncewb5L0NL8zF8dkFwJxx6XtWQA5glJ4C9pyp5JE5PGWgoEN1k37hXnDxuP8a+GSxF5XFeitLNDi8fL5z93gG4rayouyvMpzYmZ29CwKYgDd6uh73SQAUwIDAQAB";
    private static final String ITEM_SKU="donate";
    private static final String ITEM_SKU_2="donate2";
    private static final String ITEM_SKU_4="donate4";
    private static final String ITEM_SKU_6="donate6";
    private static final String ITEM_SKU_8="donate8";
    private static final String ITEM_SKU_10="donate10";

    private IabHelper mHelper;


    /**
     * PayPal
     */
    private static final String PAYPAL_ADDRESS = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=X2SCFSHBXUMUC&lc=GB&item_name=Donate&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest";

    /**
     * Bitcoin
     */
    private static final String BITCOIN_ADDRESS = "1JwQYRiGm7JBuWSaxuVrFvatHTwJ5mzAdm";

    /**
     * alipay https://qr.alipay.com/a6x00650jmlcyr8ug5faf92
     */
    private static final String APLPAY_QRADDRESS="https://raw.githubusercontent.com/x-falcon/tools/master/a.png";
    private static final String APLPAY_ADDRESS = "https://qr.alipay.com/aex00155nkzbj7tuxj3vw38";
    private static final String APLPAY_INTENT_URI_FORMAT = "intent://platformapi/startapp?saId=10000007&" +
            "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2F{payCode}%3F_s" +
            "%3Dweb-other&_t=1472443966571#Intent;" +
            "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
    private static final String APLPAY_PAYCODE = "aex00155nkzbj7tuxj3vw38";

    /**
     * wechat pay
     */
    private static final String WECHATPAY_QRADDRESS="https://raw.githubusercontent.com/x-falcon/tools/master/w.png";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        CardView cv_alipay =(CardView)findViewById(R.id.cv_alipay);
        CardView cv_wexin =(CardView)findViewById(R.id.cv_wexin);
        CardView cv_paypal =(CardView)findViewById(R.id.cv_paypal);
        CardView cv_bit =(CardView)findViewById(R.id.cv_bit);
        CardView cv_google =(CardView)findViewById(R.id.cv_google);
        CardView cv_google2 =(CardView)findViewById(R.id.cv_google2);
        CardView cv_google4 =(CardView)findViewById(R.id.cv_google4);
        CardView cv_google6 =(CardView)findViewById(R.id.cv_google6);
        CardView cv_google8 =(CardView)findViewById(R.id.cv_google8);
        CardView cv_google10 =(CardView)findViewById(R.id.cv_google10);

        if(IS_GooglePlay){
            cv_alipay.setVisibility(View.GONE);
            cv_wexin.setVisibility(View.GONE);
            cv_paypal.setVisibility(View.GONE);
            cv_bit.setVisibility(View.GONE);
            cv_google.setVisibility(View.GONE);
        }else{
            cv_google2.setVisibility(View.GONE);
            cv_google4.setVisibility(View.GONE);
            cv_google6.setVisibility(View.GONE);
            cv_google8.setVisibility(View.GONE);
            cv_google10.setVisibility(View.GONE);
        }

        final Button button_rate = (Button) findViewById(R.id.bt_rate);
        final Button button_alipay = (Button) findViewById(R.id.bt_alipay);
        final Button button_wechat = (Button) findViewById(R.id.bt_weixin);
        final Button button_paypal = (Button) findViewById(R.id.bt_paypal);
        final Button button_google = (Button) findViewById(R.id.bt_google);
        final Button button_google2 = (Button) findViewById(R.id.bt_google2);
        final Button button_google4 = (Button) findViewById(R.id.bt_google4);
        final Button button_google6 = (Button) findViewById(R.id.bt_google6);
        final Button button_google8 = (Button) findViewById(R.id.bt_google8);
        final Button button_google10 = (Button) findViewById(R.id.bt_google10);
        final Button button_bit = (Button) findViewById(R.id.bt_bit);
        mHelper = new IabHelper(this, GOOGLE_PUBKEY);
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {

                if (!result.isSuccess()) {
                    LogUtils.d(TAG, "Problem setting up in-app billing: " + result);
                    return;
                } else {
                    button_google.setEnabled(true);
                    button_google2.setEnabled(true);
                    button_google4.setEnabled(true);
                    button_google6.setEnabled(true);
                    button_google8.setEnabled(true);
                    button_google10.setEnabled(true);
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;
            }
        });
        button_rate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rateOnStore(v);
            }
        });
        button_alipay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateAlipayOnclick(v);
            }
        });

        button_wechat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateWechatOnclick(v);
            }
        });

        button_paypal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donatePayPalOnClick(v);
            }
        });
        button_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v,ITEM_SKU);
            }
        });
        button_google2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v,ITEM_SKU_2);
            }
        });
        button_google4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v,ITEM_SKU_4);
            }
        });
        button_google6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v,ITEM_SKU_6);
            }
        });
        button_google8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v,ITEM_SKU_8);
            }
        });
        button_google10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v,ITEM_SKU_10);
            }
        });
        button_bit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateBitcoinOnClick(v);
            }
        });
        button_bit.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                return donateBitcoinOnLongClick(v);
            }
        });

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

    private void rateOnStore(View view){
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(STORE_URI));
            startActivity(intent);
        }catch (Exception e){
            openBrowser(STORE_URI2);
        }
    }
    private void donateAlipayOnclick(View view) {
        try {
            startActivity(Intent.parseUri(APLPAY_INTENT_URI_FORMAT.replace("{payCode}", APLPAY_PAYCODE),Intent.URI_INTENT_SCHEME));
        } catch (Exception e) {
            openBrowser(APLPAY_QRADDRESS);
        }
    }

    public void donatePayPalOnClick(View view) {
        try {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW,Uri.parse(PAYPAL_ADDRESS));
            startActivity(viewIntent);
        } catch (ActivityNotFoundException e) {
            LogUtils.d(TAG, e.toString(), e);
        }
    }

    private void donateWechatOnclick(View view){
        openBrowser(WECHATPAY_QRADDRESS);
    }

    /**
     * Donate with bitcoin by opening a bitcoin: intent if available.
     */
    public void donateBitcoinOnClick(View view) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("bitcoin:" + BITCOIN_ADDRESS)));
        } catch (ActivityNotFoundException e) {
            view.performLongClick();
        }
    }

    public boolean donateBitcoinOnLongClick(View view) {
        ClipboardManager clipboard = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(BITCOIN_ADDRESS, BITCOIN_ADDRESS);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.tip_bitcoin, Toast.LENGTH_SHORT).show();
        return true;
    }

    public void donateGoogleOnClick(View view,String item_sku) {
        try {
            mHelper.launchPurchaseFlow(this,
                    item_sku,PLAY_PURCHASE_REQUEST_CODE , mPurchaseFinishedListener, null);
        } catch (Exception e) {

        }

    }

    private void openBrowser(String url){
        try {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
        }catch (Exception e){
            LogUtils.d(TAG,"UNKNOW ERROR");
        }
    }

    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (mHelper == null) return;

            if (result.isSuccess()) {
                // directly consume in-app purchase, so that people can donate multiple times
                try {
                    mHelper.consumeAsync(purchase, mConsumeFinishedListener);

                } catch (Exception e) {

                }
            }
        }
    };
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isSuccess()) {
                LogUtils.d(TAG, "success");
            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            LogUtils.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }
}