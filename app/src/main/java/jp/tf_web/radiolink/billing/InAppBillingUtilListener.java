package jp.tf_web.radiolink.billing;

import jp.tf_web.radiolink.billing.util.IabResult;
import jp.tf_web.radiolink.billing.util.Inventory;
import jp.tf_web.radiolink.billing.util.Purchase;

/**
 * Created by furukawanobuyuki on 2015/12/30.
 */
public interface InAppBillingUtilListener {

    /** セットアップ結果の通知
     *
     * @param result
     */
    public void onIabSetupFinished(IabResult result);

    /** 購入済みアイテムの取得結果の通知
     *
     * @param result
     * @param inv
     */
    public void onQueryInventoryFinished(IabResult result, Inventory inv);

    /** 購入完了 結果の通知
     *
     * @param result
     * @param purchase
     */
    public void onIabPurchaseFinished(IabResult result, Purchase purchase);
}
