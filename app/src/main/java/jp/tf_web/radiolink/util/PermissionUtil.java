package jp.tf_web.radiolink.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/** パーミッション確認
 *
 * Created by furukawanobuyuki on 2016/01/26.
 */
public class PermissionUtil {

    //リクエストコード
    public static final int REQUEST_CODE = 0;

    private static PermissionUtil ourInstance = new PermissionUtil();

    public static PermissionUtil getInstance() {
        return ourInstance;
    }

    private PermissionUtil() {
    }


    /** パーミッション確認
     *
     * @param activity
     * @param permissions
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean checkPermissions(final Activity activity,List<String> permissions){
        boolean result = false;

        List<String> requestPermissions = new ArrayList<String>();
        for(String permission:permissions){
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // permissionが許可されていません
                if (activity.shouldShowRequestPermissionRationale(permission)) {
                    // 許可ダイアログで今後表示しないにチェックされていない場合
                }
                requestPermissions.add(permission);
            }
        }

        if(requestPermissions.size() > 0) {
            //TODO: permissionを許可してほしい理由の表示

            // 許可ダイアログの表示
            activity.requestPermissions(requestPermissions.toArray(new String[0]), REQUEST_CODE);

            result = true;
        }

        return result;
    }

    /** 許可ダイアログに対してユーザーが選択した結果の受け取り
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE:
                /*
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //ユーザーが許可したとき
                }
                else{
                    //ユーザーが許可しなかったとき
                }
                */
                break;
        }
    }
}
