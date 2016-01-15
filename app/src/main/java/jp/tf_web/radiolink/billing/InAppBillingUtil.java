package jp.tf_web.radiolink.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.billing.util.IabHelper;
import jp.tf_web.radiolink.billing.util.IabResult;
import jp.tf_web.radiolink.billing.util.Inventory;
import jp.tf_web.radiolink.billing.util.Purchase;

/** 課金処理をする為のユーテリティ
 *
 * Created by furukawanobuyuki on 2015/12/30.
 */
public class InAppBillingUtil {
    private static String TAG = "InAppBillingUtil";

    //課金処理ヘルパー
    private IabHelper iabHelper;

    //コンテキスト
    private Context context;

    //イベント通知先リスナー
    private InAppBillingUtilListener listener;

    //リクエストコード
    private int requestCode = 10001;

    /** コンストラクタ
     *
     * @param context コンテキスト
     * @param listener イベント通知先リスナー
     */
    public InAppBillingUtil(final Context context,final InAppBillingUtilListener listener){
        this.context = context;
        this.listener = listener;

        //ヘルパーインスタンスの生成
        iabHelper = new IabHelper(this.context, Config.GOOGLE_PLAY_PUBLICKEY);
    }

    /** 課金処理のセットアップ
     *
     */
    public void setup(){
        // 購入リクエスト前の準備
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d("IAB", "セットアップ完了");

                if (iabHelper == null) return;

                //リスナーに通知
                listener.onIabSetupFinished(result);

                if (!result.isSuccess()) {
                    Log.d("IAB", "セットアップ失敗");
                    return;
                }

                Log.d(TAG, "購入済みアイテムを取得する");
                iabHelper.queryInventoryAsync(gotInventoryListener);
            }
        });
    }

    /** 破棄
     *
     */
    public void dispose() {
        if(iabHelper == null) return;

        iabHelper.dispose();
        iabHelper = null;
    }

    private IabHelper.QueryInventoryFinishedListener gotInventoryListener = new IabHelper.QueryInventoryFinishedListener(){

        //購入済みアイテムの取得 完了
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inv) {
            Log.d("IAB", "購入済みアイテムの取得完了");

            if (iabHelper == null) return;

            //購入済みアイテムの取得 結果の通知
            listener.onQueryInventoryFinished(result,inv);

            /*
            if (result.isFailure()) {
                Log.d("IAB", "購入済みアイテムの取得失敗");
                return;
            }

            Log.d("IAB", "購入済みアイテムの取得成功");

            // 購入済みアイテムの確認
            Purchase purchase = inv.getPurchase(Config.PRODUCT_ITEM_1_ID);
            if (purchase != null) {
                Log.d("IAB", "商品を購入済みです。");
            }
            */
        }
    };


    /** 購入ボタンを押した時に購入リクエストを送る
     *
     * @param activity
     * @param productItemId
     * @return
     */
    public boolean onBuyButtonClicked(final Activity activity,final String productItemId) {

        // 端末がサブスクリプション課金に対応しているかを確認する
        if (!iabHelper.subscriptionsSupported()) {
            Log.d(TAG,"あなたの端末ではサブスクリプション購入はできません。");
            return false;
        }

        Log.d("IAB", "購入処理を開始");
        try {
            iabHelper.launchPurchaseFlow(activity,
                                            productItemId,
                                            requestCode,
                                            purchaseFinishedListener,
                                            null);
        }
        catch (IllegalStateException ex) {
            Log.d(TAG, "例外：購入処理中です。");
            return false;
        }

        return true;
    }

    // 購入結果の受け取り用メソッド
    IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d("IAB", "購入完了 result:" + result + ", purchase: " + purchase);

            if (iabHelper == null) return;

            //リスナーに通知
            listener.onIabPurchaseFinished(result,purchase);

            /*
            if (result.isFailure()) {
                Log.d("IAB","購入失敗");
                return;
            }
            Log.d("IAB", "購入成功");

            if (purchase.getSku().equals(Config.PRODUCT_ITEM_1_ID)) {
                Log.d("IAB", "あなたの商品：" + Config.PRODUCT_ITEM_1_ID + "を購入しました。");
                Log.d("IAB","orderIdは：" + purchase.getOrderId());
                Log.d("IAB","INAPP_PURCHASE_DATAのJSONは：" + purchase.getOriginalJson());
            }
            */
        }
    };

    /** Activity で受け取ったイベントの通知先
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return iabHelper.handleActivityResult(requestCode, resultCode, data);
    }
}
