package com.dooboolab.iapexample;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchaseHistoryResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.android.vending.billing.IInAppBillingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

  private String TAG = "MainActivity";
  private Boolean prepared = false;

  private Button btn1;
  private Button btn2;
  private Button btn3;
  private Button btn4;

  private IInAppBillingService mService;
  private final String BASE64_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiNdnKt3hfOkWkzgo4LllkzmvvdjZtxZbeHgkj7ccxIe3Jdd0x2IqIM1ZwzvNgmDSaBkUXJMOZV9nWuS6Dalq3lPViJwNPgf2gaWJ6j9RXVSZNfugbp8svFDmbZCDy5phCmFxwLRsllCkq9yCnDlE2SS0ZjnsD+scll4aIZsyEdotXt4xKdyl+xDbUPOCVfU9rLzTfrSnUig8Ed92aesMYWWQPoCI9Yhl/BAl0tJRf2BVIXtB1W95sns0wcABSt6rz3+B97XhgnmnA/A/kvKdytt4kNxdVQroF9bbZpITCd4KvavKccom4MEV0XtrUPRyholvBtDcXO+xt8S7ldu7RQIDAQAB";

  ServiceConnection mServiceConn = new ServiceConnection() {
    @Override public void onServiceDisconnected(ComponentName name) {
      mService = null;
    }
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mService = IInAppBillingService.Stub.asInterface(service);
    }
  };

  private BillingClient mBillingClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    btn1 = (Button) findViewById(R.id.btn1);
    btn2 = (Button) findViewById(R.id.btn2);
    btn3 = (Button) findViewById(R.id.btn3);
    btn4 = (Button) findViewById(R.id.btn4);

    btn1.setOnClickListener(this);
    btn2.setOnClickListener(this);
    btn3.setOnClickListener(this);
    btn4.setOnClickListener(this);

    prepare();
  }

  public void prepare() {
    Intent intent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
    // This is the key line that fixed everything for me
    intent.setPackage("com.android.vending");

    getApplicationContext().bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);

    mBillingClient = BillingClient.newBuilder(this).setListener(purchasesUpdatedListener).build();
    mBillingClient.startConnection(billingClientStateListener);
  }

  public void refreshPurchaseItems() {
    try {
      Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
      int response = ownedItems.getInt("RESPONSE_CODE");
      if (response == 0) {
        ArrayList
            purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        String[] tokens = new String[purchaseDataList.size()];
        for (int i = 0; i < purchaseDataList.size(); ++i) {
          String purchaseData = (String) purchaseDataList.get(i);
          JSONObject jo = new JSONObject(purchaseData);
          tokens[i] = jo.getString("purchaseToken");
          mService.consumePurchase(3, getPackageName(), tokens[i]);
        }
      }

      // 토큰을 모두 컨슘했으니 구매 메서드 처리
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void getItems() {
    if (!prepared || mService == null) {
      Log.d(TAG, "IAP not prepared. Please restart your app again.");
      return;
    }

    ArrayList<String> skuList = new ArrayList<> ();
    skuList.add("point_1000");
    skuList.add("5000_point");
    skuList.add("10000_point");
    SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
    params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
    mBillingClient.querySkuDetailsAsync(params.build(),
      new SkuDetailsResponseListener() {
        @Override
        public void onSkuDetailsResponse(int responseCode, List<SkuDetails> skuDetailsList) {
          Log.d(TAG, "responseCode: " + responseCode);
          Log.d(TAG, skuDetailsList.toString());
        }
      }
    );
  }

  public void buyItem(String id_item) {
    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
        .setSku(id_item)
        .setType(BillingClient.SkuType.INAPP)
        .build();

    int responseCode = mBillingClient.launchBillingFlow(this, flowParams);
    Log.d(TAG, "buyItem responseCode: " + responseCode);
  }

  public void getOwnedItems() {
    if (!prepared || mService == null) {
      Log.d(TAG, "IAP not prepared. Please restart your app again.");
      return;
    }

    Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
    mBillingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, purchaseHistoryListener);
  }

  public void consumeItem(String token) {
    mBillingClient.consumeAsync(token, consumeResponseListener);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.btn1:
        // buyItem("android.test.purchased");
        buyItem("point_1000");
        break;
      case R.id.btn2:
          buyItem("5000_point");
        break;
      case R.id.btn3:
          // buyItem("4634419232429048426");
        buyItem("android.test.purchased");
        break;
      case R.id.btn4:
          getItems();
        break;
    }
  }

  @Override public void onDestroy() {
    super.onDestroy();
    try {
      if (mServiceConn != null) {
        unbindService(mServiceConn);
      }
    } catch (IllegalArgumentException ie) {
      Log.e(TAG, "IllegalArgumentException");
      Log.e(TAG, ie.getMessage());
    }
  }

  BillingClientStateListener billingClientStateListener = new BillingClientStateListener() {
    @Override
    public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
      if (billingResponseCode == BillingClient.BillingResponse.OK) {
        // The billing client is ready.
        Log.d(TAG, "billing client ready");
        prepared = true;
        refreshPurchaseItems();
      }
    }
    @Override
    public void onBillingServiceDisconnected() {
      // Try to restart the connection on the next request to
      // Google Play by calling the startConnection() method.
      Log.d(TAG, "billing client disconnected");
      prepared = false;
      mBillingClient.startConnection(this);
    }
  };

  ConsumeResponseListener consumeResponseListener = new ConsumeResponseListener() {
    @Override
    public void onConsumeResponse(@BillingClient.BillingResponse int responseCode, String outToken) {
      if (responseCode == BillingClient.BillingResponse.OK) {
        // Handle the success of the consume operation.
        // For example, increase the number of coins inside the user's basket.
        Log.d(TAG, "consume responseCode: " + responseCode);
      }
    }
  };

  PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
      Log.d(TAG, "Purchase Updated Listener");
      Log.d(TAG, "responseCode: " + responseCode);
      if (responseCode == 0) {
        Log.d(TAG, purchases.toString());
        consumeItem(purchases.get(0).getPurchaseToken());
      }
    }
  };

  PurchaseHistoryResponseListener purchaseHistoryListener = new PurchaseHistoryResponseListener() {
    @Override
    public void onPurchaseHistoryResponse(@BillingClient.BillingResponse int responseCode,
                                          List<Purchase> purchasesList) {
      if (responseCode == BillingClient.BillingResponse.OK
          && purchasesList != null) {
        for (Purchase purchase : purchasesList) {
          // Process the result.
          Log.d(TAG, "purchase");
          Log.d(TAG, purchase.toString());
        }
      }
    }
  };
}
