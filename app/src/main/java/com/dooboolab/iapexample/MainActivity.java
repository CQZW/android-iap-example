package com.dooboolab.iapexample;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.android.vending.billing.IInAppBillingService;
import com.dooboolab.iapexample.util.IabHelper;
import com.dooboolab.iapexample.util.IabResult;
import com.dooboolab.iapexample.util.Purchase;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

  private String TAG = "MainActivity";
  private final int RC_REQUEST = 10001;

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
          // Oh noes, there was a problem.
          Log.d(TAG, "Problem setting up in-app billing: " + result);
          return;
        }
        AlreadyPurchaseItems();

        // Have we been disposed of in the meantime? If so, quit.
        if (mHelper == null) return;

        // Important: Dynamically register for broadcast messages about updated purchases.
        // We register the receiver here instead of as a <receiver> in the Manifest
        // because we always call getPurchases() at startup, so therefore we can ignore
        // any broadcasts sent while the app isn't running.
        // Note: registering this listener in an Activity is a bad idea, but is done here
        // because this is a SAMPLE. Regardless, the receiver must be registered after
        // IabHelper is setup, but before first call to getPurchases().
//        mBroadcastReceiver = new IabBroadcastReceiver(MainActivity.this);
//        IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
//        registerReceiver(mBroadcastReceiver, broadcastFilter);
      }
    });
  }

  public void AlreadyPurchaseItems() {
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

  public void Buy(String id_item) {
    try{
      Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), id_item, "inapp", "test");
      PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");

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
      Log.e(TAG, "Buy error");
      Log.e(TAG, e.getMessage());
    }
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.btn1:
          Buy("4636942031015148831");
        break;
      case R.id.btn2:
          Buy("4636168153771692423");
        break;
      case R.id.btn3:
          Buy("4634419232429048426");
        break;
      case R.id.btn4:
          Buy("android.test.purchased");
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
      try {
        mService.consumePurchase(3, getPackageName(), purchase.getToken());
      } catch (RemoteException re) {
        Log.e(TAG, "RemoteException");
        Log.e(TAG, re.getMessage());
      }
      // 만약 서버로 영수증 체크후에 아이템 추가한다면,
      // 서버로 purchase.getOriginalJson() , purchase.getSignature() 2개 보내시면 됩니다.
    }
  };
}
