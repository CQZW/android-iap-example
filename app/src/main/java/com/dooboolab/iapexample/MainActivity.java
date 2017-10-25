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
import com.android.billingclient.api.Purchase;
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
  private final int RC_REQUEST = 10001;

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
    skuList.add("4636942031015148831");
    skuList.add("5000P");
    skuList.add("10000P");
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


//          for (String thisResponse : responseList) {
//            try {
//              JSONObject object = new JSONObject(thisResponse);
//
//              String sku   = object.getString("productId");
//              String title = object.getString("title");
//              String price = object.getString("price");
//
//              Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\":\"productId\" return " + sku);
//              Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\":\"title\" return " + title);
//              Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\":\"price\" return " + price);
//
//              if (!sku.equals("android.test.purchased")) continue;
//
//              Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");
//
//              Toast.makeText(getApplicationContext(), "getBuyIntent() - success return Bundle", Toast.LENGTH_SHORT).show();
//              Log.i(TAG, "getBuyIntent() - success return Bundle");
//
//              response = buyIntentBundle.getInt("RESPONSE_CODE");
//              Toast.makeText(getApplicationContext(), "getBuyIntent() - \"RESPONSE_CODE\" return " + String.valueOf(response), Toast.LENGTH_SHORT).show();
//              Log.i(TAG, "getBuyIntent() - \"RESPONSE_CODE\" return " + String.valueOf(response));
//            } catch (JSONException e) {
//              e.printStackTrace();
//            } catch (RemoteException e) {
//              e.printStackTrace();
//
//              Toast.makeText(getApplicationContext(), "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
//              Log.w(TAG, "getBuyIntent() - fail!");
//            } catch (Exception e) {
//              e.printStackTrace();
//            }
//          }
//        } catch (RemoteException re) {
//          Log.d(TAG, "RemoteException");
//          Log.d(TAG, re.getMessage());
//          Toast.makeText(getApplicationContext(), "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
//        }
  }

  public void buyItem(String id_item) {
    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
        .setSku(id_item)
        .setType(BillingClient.SkuType.INAPP)
        .build();

    int responseCode = mBillingClient.launchBillingFlow(this, flowParams);
    try{
      Bundle buyItemIntentBundle = mService.getBuyIntent(3, getPackageName(), id_item, "inapp", "test");
      PendingIntent pendingIntent = buyItemIntentBundle.getParcelable("buyItem_INTENT");

      if (pendingIntent != null) {
        startIntentSenderForResult(
            pendingIntent.getIntentSender(),
            RC_REQUEST,
            new Intent(),
            Integer.valueOf(0),
            Integer.valueOf(0),
            Integer.valueOf(0)
        );
        // mHelper.launchPurchaseFlow(this, getPackageName(), RC_REQUEST, mPurchaseFinishedListener, "test");
      }
    } catch (Exception e) {
      Log.e(TAG, "buyItem error");
      Log.e(TAG, e.getMessage());
    }
  }

  public void getOwnedItems() {
    if (!prepared || mService == null) {
      Log.d(TAG, "IAP not prepared. Please restart your app again.");
      return;
    }

    try {
      Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

      int response = ownedItems.getInt("RESPONSE_CODE");
      if (response == 0) {
        ArrayList<String> ownedSkus =
            ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
        ArrayList<String>  purchaseDataList =
            ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
        ArrayList<String>  signatureList =
            ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE_LIST");
        String continuationToken =
            ownedItems.getString("INAPP_CONTINUATION_TOKEN");

        for (int i = 0; i < purchaseDataList.size(); ++i) {
          String purchaseData = purchaseDataList.get(i);
          String signature = signatureList.get(i);
          String sku = ownedSkus.get(i);

          // do something with this purchase information
          // e.g. display the updated list of products owned by user
        }

        // if continuationToken != null, call getPurchases again
        // and pass in the token to retrieve more items
      }
    } catch (RemoteException re) {
      Log.d(TAG, "RemoteException");
      Log.d(TAG, re.getMessage());
      Toast.makeText(getApplicationContext(), "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
    }
  }

  public void consumeItem(String token) {
    try {
      mService.consumePurchase(3, getPackageName(), token);
    } catch (RemoteException re) {
      Log.e(TAG, "RemoteException");
      Log.e(TAG, re.getMessage());
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.btn1:
        buyItem("android.test.purchased");
        // buyItem("4636942031015148831");
        break;
      case R.id.btn2:
          buyItem("4636168153771692423");
        break;
      case R.id.btn3:
          buyItem("4634419232429048426");
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

  PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
    @Override
    public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
      Log.d(TAG, "Purcase Updated Listener");
      Log.d(TAG, "responseCode: " + responseCode);
      Log.d(TAG, purchases.toString());
    }
  };
}
