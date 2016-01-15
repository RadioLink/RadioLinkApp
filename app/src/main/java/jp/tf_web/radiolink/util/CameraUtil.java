package jp.tf_web.radiolink.util;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jp.tf_web.radiolink.R;

/**
 * Created by furukawanobuyuki on 2016/01/13.
 */
public class CameraUtil {
    private static String TAG = "CameraUtil";

    //リクエストコード
    private static int requestCode = 20001;

    //カメラで撮影した写真
    private static File captchaFile;

    /** 画像ファイル名を生成
     *
     * @param context
     * @return
     */
    private static String createImageFileName(final Context context){
        return context.getString(R.string.app_name)+"_"+System.currentTimeMillis()+".jpg";
    }

    /**カメラ,ギャラリーから写真を取得するインテントを発行
     *
     * @param activity
     */
    public static void requestGetImage(final Activity activity){

        //ギャラリー起動インテント
        Intent pickPhotoIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickPhotoIntent.setType("image/*");

        //カメラ起動インテント
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        captchaFile = new File( Environment.getExternalStorageDirectory(), createImageFileName(activity.getApplicationContext()));
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(captchaFile));

        //2つ設定して選択させる
        Intent chooserIntent = Intent.createChooser( pickPhotoIntent, "Picture...");
        chooserIntent.putExtra( Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePhotoIntent});
        activity.startActivityForResult(chooserIntent, requestCode);

    }

    /** 写真取得時の通知か確認
     *
     * @param activity
     * @param requestCode
     * @param resultCode
     * @param data
     * @param listener
     * @return
     */
    public static boolean onActivityResult(final Activity activity,int requestCode, int resultCode, Intent data,final CameraUtilListener listener) {
        boolean result = false;
        if(CameraUtil.requestCode == requestCode){
            Log.d(TAG, "CameraUtil requestCode:"+requestCode+" data:"+data);
            if(resultCode == Activity.RESULT_OK){
                //写真データの取得できたのでBitmapを作成
                result = true;

                Bitmap bitmap = null;
                if(data == null){
                    //Log.d(TAG, "カメラで撮影");
                    if(captchaFile != null) {
                        try {
                            //ファイルサイズが多いきすぎるので縮小する
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 2;

                            bitmap = BitmapFactory.decodeStream(new FileInputStream(captchaFile), null, options);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    //Log.d(TAG, "ギャラリーから写真 選択");
                    try {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;

                        InputStream stream = activity.getContentResolver().openInputStream(data.getData());
                        bitmap = BitmapFactory.decodeStream(stream, null, options);
                        stream.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //Listener通知
                listener.onImage(bitmap);
            }
        }
        return result;
    }
}
