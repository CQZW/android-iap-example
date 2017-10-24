package com.dooboolab.iapexample;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.dooboolab.iapexample.util.IabHelper;
import com.dooboolab.iapexample.util.IabResult;
import com.dooboolab.iapexample.util.Purchase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

  private String TAG = "MainActivity";
  private final int RC_REQUEST = 10001;

  private Boolean prepared;

  private Button btn1;
  private Button btn2;
  private Button btn3;
  private Button btn4;

  private IInAppBillingService mService;
  private IabHelper mHelper;
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

    mHelper = new IabHelper(this, BASE64_KEY);
    mHelper.enableDebugLogging(true);
    mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
      public void onIabSetupFinished(IabResult result) {
        Log.d(TAG, "Setup finished.");

        if (!result.isSuccess()) {
          Log.d(TAG, "Problem setting up in-app billing: " + result);
          prepared = false;
          return;
        }
        refreshPurchaseItems();

        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) {
          prepared = false;
          return;
        }

        prepared = true;
      }
    });
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
    Bundle querySkus = new Bundle();
    querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

    try {
      Bundle skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
      Toast.makeText(getApplicationContext(), "getSkuDetails() - success return Bundle", Toast.LENGTH_SHORT).show();

      int response = skuDetails.getInt("RESPONSE_CODE");
      Toast.makeText(getApplicationContext(), "getSkuDetails() - \"RESPONSE_CODE\" return " + String.valueOf(response), Toast.LENGTH_SHORT).show();
      Log.i(TAG, "getSkuDetails() - \"RESPONSE_CODE\" return " + String.valueOf(response));

      if (response != 0) return;

      ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
      Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\" return " + responseList.toString());

      if (responseList.size() == 0) return;

      for (String thisResponse : responseList) {
        try {
          JSONObject object = new JSONObject(thisResponse);

          String sku   = object.getString("productId");
          String title = object.getString("title");
          String price = object.getString("price");

          Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\":\"productId\" return " + sku);
          Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\":\"title\" return " + title);
          Log.i(TAG, "getSkuDetails() - \"DETAILS_LIST\":\"price\" return " + price);

          if (!sku.equals("android.test.purchased")) continue;

          Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");

          Toast.makeText(getApplicationContext(), "getBuyIntent() - success return Bundle", Toast.LENGTH_SHORT).show();
          Log.i(TAG, "getBuyIntent() - success return Bundle");

          response = buyIntentBundle.getInt("RESPONSE_CODE");
          Toast.makeText(getApplicationContext(), "getBuyIntent() - \"RESPONSE_CODE\" return " + String.valueOf(response), Toast.LENGTH_SHORT).show();
          Log.i(TAG, "getBuyIntent() - \"RESPONSE_CODE\" return " + String.valueOf(response));
        } catch (JSONException e) {
          e.printStackTrace();
        } catch (RemoteException e) {
          e.printStackTrace();

          Toast.makeText(getApplicationContext(), "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
          Log.w(TAG, "getBuyIntent() - fail!");
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    } catch (RemoteException re) {
      Log.d(TAG, "RemoteException");
      Log.d(TAG, re.getMessage());
      Toast.makeText(getApplicationContext(), "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
    }
  }

  public void buyItem(String id_item) {
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
        mHelper.launchPurchaseFlow(this, getPackageName(), RC_REQUEST, mPurchaseFinishedListener, "test");
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

  // 방법 1
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Log.d(TAG, "requestCode: " + requestCode);
    Log.d(TAG, "resultCode: " + resultCode);
    if (requestCode == RC_REQUEST) {
      if (resultCode == RESULT_OK) {
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
          super.onActivityResult(requestCode, resultCode, data);

          int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
          String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
          String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

          return;
        }
      }
    }
    // 구매 취소 처리
    Log.d(TAG, "payment cancelled");
  }

  // 방법 2
  IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener  = new IabHelper.OnIabPurchaseFinishedListener() {
    public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
      Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
      consumeItem(purchase.getToken());
      // 만약 서버로 영수증 체크후에 아이템 추가한다면,
      // 서버로 purchase.getOriginalJson() , purchase.getSignature() 2개 보내시면 됩니다.
    }
  };
}
