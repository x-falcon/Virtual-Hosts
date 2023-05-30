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
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.android.billingclient.api.*;
import com.github.xfalcon.vhosts.util.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DonationActivity extends AppCompatActivity {

    private final String TAG = DonationActivity.class.getSimpleName();
    /**
     * Google
     */
    private static final boolean IS_GooglePlay=BuildConfig.IS_GooglePlay;
    private static final int PLAY_PURCHASE_REQUEST_CODE = 0x15;
    private static final String STORE_URI2 = "https://play.google.com/store/apps/details?id=com.github.xfalcon.vhosts";
    private static final String STORE_URI = "market://details?id=com.github.xfalcon.vhosts";
    private static final String GOOGLE_PUBKEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxic/nGZvXgn3O0Sq8kSZCMZ0g6I1VYm1AoT8FlXegHJ1Fm+PAZ0Na0NLEXnP7okdxlRuUsO9SmUH4lv0Mz3sY0RXwIPCchiCMUn8wKGXtKY4Bh+R3xdt8DLDBc+h4i9jZYnVW1ntv4yuQrcowrUHQauRW0+sv+AGQmJuJdASCqwLgMBGa4uZh0uDmtTTfhEFaHmaUHlRBA4EgrABIJCF5QDQyYYDx75pncewb5L0NL8zF8dkFwJxx6XtWQA5glJ4C9pyp5JE5PGWgoEN1k37hXnDxuP8a+GSxF5XFeitLNDi8fL5z93gG4rayouyvMpzYmZ29CwKYgDd6uh73SQAUwIDAQAB";
    private static final String ITEM_SKU = "donate";
    private static final String ITEM_SKU_2 = "donate2";
    private static final String ITEM_SKU_4 = "donate4";
    private static final String ITEM_SKU_6 = "donate6";
    private static final String ITEM_SKU_8 = "donate8";
    private static final String ITEM_SKU_10 = "donate10";

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
    private static final String APLPAY_QRADDRESS = "https://raw.githubusercontent.com/x-falcon/tools/master/a.png";
    private static final String APLPAY_ADDRESS = "https://qr.alipay.com/aex00155nkzbj7tuxj3vw38";
    private static final String APLPAY_INTENT_URI_FORMAT = "intent://platformapi/startapp?saId=10000007&" +
            "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2F{payCode}%3F_s" +
            "%3Dweb-other&_t=1472443966571#Intent;" +
            "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
    private static final String APLPAY_PAYCODE = "aex00155nkzbj7tuxj3vw38";

    /**
     * wechat pay
     */
    private static final String WECHATPAY_QRADDRESS = "https://raw.githubusercontent.com/x-falcon/tools/master/w.png";


    private BillingClient billingClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donation);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        CardView cv_alipay = (CardView) findViewById(R.id.cv_alipay);
        CardView cv_wexin = (CardView) findViewById(R.id.cv_wexin);
        CardView cv_paypal = (CardView) findViewById(R.id.cv_paypal);
        CardView cv_bit = (CardView) findViewById(R.id.cv_bit);
        CardView cv_google = (CardView) findViewById(R.id.cv_google);
        CardView cv_google2 = (CardView) findViewById(R.id.cv_google2);
        CardView cv_google4 = (CardView) findViewById(R.id.cv_google4);
        CardView cv_google6 = (CardView) findViewById(R.id.cv_google6);
        CardView cv_google8 = (CardView) findViewById(R.id.cv_google8);
        CardView cv_google10 = (CardView) findViewById(R.id.cv_google10);

        if (IS_GooglePlay) {
            cv_alipay.setVisibility(View.GONE);
            cv_wexin.setVisibility(View.GONE);
            cv_paypal.setVisibility(View.GONE);
            cv_bit.setVisibility(View.GONE);
            cv_google.setVisibility(View.GONE);
        } else {
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

        initializeBillClient();

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
                donateGoogleOnClick(v, ITEM_SKU);
            }
        });
        button_google2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v, ITEM_SKU_2);
            }
        });
        button_google4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v, ITEM_SKU_4);
            }
        });
        button_google6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v, ITEM_SKU_6);
            }
        });
        button_google8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v, ITEM_SKU_8);
            }
        });
        button_google10.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                donateGoogleOnClick(v, ITEM_SKU_10);
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

    public void initializeBillClient() {

        billingClient = BillingClient.newBuilder(this).enablePendingPurchases().setListener(
                (billingResult, list) -> {

                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                        LogUtils.d(TAG, "BillingResult Response is OK");
                        for (Purchase purchase : list) {

                            LogUtils.d(TAG, "BillingResult Response is OK");
                        }
                    } else {

                        LogUtils.d(TAG, "BillingResult Response NOT OK");
                    }
                }
        ).build();

        establishConnection();
    }

    void establishConnection() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    final Button button_google = (Button) findViewById(R.id.bt_google);
                    final Button button_google2 = (Button) findViewById(R.id.bt_google2);
                    final Button button_google4 = (Button) findViewById(R.id.bt_google4);
                    final Button button_google6 = (Button) findViewById(R.id.bt_google6);
                    final Button button_google8 = (Button) findViewById(R.id.bt_google8);
                    final Button button_google10 = (Button) findViewById(R.id.bt_google10);
                    button_google.setEnabled(true);
                    button_google2.setEnabled(true);
                    button_google4.setEnabled(true);
                    button_google6.setEnabled(true);
                    button_google8.setEnabled(true);
                    button_google10.setEnabled(true);

                    LogUtils.d(TAG, "Connection Established");
                }else {
                    LogUtils.e(TAG, "Problem setting up in-app billing: " + billingResult.toString());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.d(TAG, "Connection NOT Established");
                establishConnection();
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

    private void rateOnStore(View view) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(STORE_URI));
            startActivity(intent);
        } catch (Exception e) {
            openBrowser(STORE_URI2);
        }
    }

    private void donateAlipayOnclick(View view) {
        try {
            startActivity(Intent.parseUri(APLPAY_INTENT_URI_FORMAT.replace("{payCode}", APLPAY_PAYCODE), Intent.URI_INTENT_SCHEME));
        } catch (Exception e) {
            openBrowser(APLPAY_QRADDRESS);
        }
    }

    public void donatePayPalOnClick(View view) {
        try {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_ADDRESS));
            startActivity(viewIntent);
        } catch (ActivityNotFoundException e) {
            LogUtils.d(TAG, e.toString(), e);
        }
    }

    private void donateWechatOnclick(View view) {
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

    public void donateGoogleOnClick(View view, String item_sku) {
        try {
            ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();

            productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(item_sku)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
            );

            QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build();

            billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
                @Override
                public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {

                    //Do Anything that you want with requested product details

                    //Calling this function here so that once products are verified we can start the purchase behavior.
                    //You can save this detail in separate variable or list to call them from any other location
                    //Create another function if you want to call this in establish connections' success state
                    LaunchPurchaseFlow(list.get(0));


                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage());
        }

    }

    void LaunchPurchaseFlow(ProductDetails productDetails) {
        ArrayList<BillingFlowParams.ProductDetailsParams> productList = new ArrayList<>();

        productList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build());

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productList)
                .build();

        billingClient.launchBillingFlow(this, billingFlowParams);
    }

    private void openBrowser(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
        } catch (Exception e) {
            LogUtils.d(TAG, "UNKNOW ERROR");
        }
    }
}
