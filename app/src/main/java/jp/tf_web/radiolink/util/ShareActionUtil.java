package jp.tf_web.radiolink.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.Menu;
import android.widget.ShareActionProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.tf_web.radiolink.Config;
import jp.tf_web.radiolink.R;

/**
 * Created by furukawanobuyuki on 2016/01/07.
 */
public class ShareActionUtil {
    private static String TAG = "ShareActionUtil";

    private static ShareActionUtil ourInstance = new ShareActionUtil();

    public static ShareActionUtil getInstance() {
        return ourInstance;
    }

    //シェアアクションプロバイダー
    private ShareActionProvider actionProvider;

    //スキーム起動時のパラメータ名
    private static String KEY_URI_PARAME_CHANNEL_CODE = "channel_code";

    private ShareActionUtil() {

    }

    /** シェアURL
     *
     * @param context
     * @param params
     * @return
     */
    public Uri createShareSchemeUri(final Context context,final Map<String,String> params){
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(context.getString(R.string.scheme));
        builder.authority(context.getString(R.string.scheme_host_share));

        for(Map.Entry<String,String> param:params.entrySet()){
            builder.appendQueryParameter(param.getKey(),param.getValue());
        }

        return builder.build();
    }

    /** シェアアクションプロバイダーを設定する
     *
     * @param actionProvider
     */
    public void setActionProvider(ShareActionProvider actionProvider){
        this.actionProvider = actionProvider;
    }

    /** ShareActionProviderにインテントの設定
     *
     * @param channel_code
     */
    public void setShareIntent(final Context context,final String channel_code){

        //共有するURI作成
        Map<String,String> params = new HashMap<String, String>(){
            {
                put(KEY_URI_PARAME_CHANNEL_CODE,channel_code);
            }
        };
        Uri uri = createShareSchemeUri(context, params);
        Log.d(TAG, "uri:" + uri.toString());

        String url = null;
        try {
            url = Config.SCHEME_REDIRECT_API + URLEncoder.encode(uri.toString(), "UTF-8");
            Log.d(TAG, "url:" + url);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //シェアタイトル
        String title = "["+context.getString(R.string.app_name)+"] "+context.getString(R.string.scheme_title);
        //シェア時のHTML
        String html = "<html>"+context.getString(R.string.app_name)+"<br>\n"
                            +"<a href='"+url+"'>"+uri+"</a></html>";

        // インテントの設定
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TITLE, title);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, html);

/* FACEBOOKはブラウザ開いて投稿すれば URLがシェアできそう

        {
            String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=http://www.google.com";
            shareIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(shareIntent);
        }
*/
        // ShareActionProviderにインテントの設定
        this.actionProvider.setShareIntent(shareIntent);
    }
}
