package jp.tf_web.radiolink.ncmb;

import android.content.Context;
import android.util.Log;

import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.LoginCallback;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

import jp.tf_web.radiolink.ncmb.db.User;
import jp.tf_web.radiolink.ncmb.listener.LoginListener;
import jp.tf_web.radiolink.ncmb.listener.LogoutListener;
import jp.tf_web.radiolink.ncmb.listener.SigninListener;

/** APIサーバとの処理の実装
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class NCMBUtil {
    private static String TAG = "NCMBUtil";

    /** コンストラクタ
     *
     * @param context コンテキスト
     * @param appKey アプリケーションキー
     * @param clientKey クライアントキー
     */
    public NCMBUtil(final Context context,final String appKey,final String clientKey){
        //NCMB 初期化
        NCMB.initialize(context, appKey, clientKey);
        //テスト
        //test();
    }

    /** 書き込みテスト
     *
     */
    public void test(){
        NCMBObject obj = new NCMBObject("TestClass");
        obj.put("message", "Hello, NCMB!");
        obj.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //保存成功
                    Log.d(TAG, "保存成功");
                } else {
                    //保存失敗
                    Log.d(TAG, "保存失敗");
                }
            }
        });
    }

    /** 新規ユーザー登録
     *
     * @param user
     */
    public void signin(final User user,final SigninListener signinListener){
        //設定したユーザ名とパスワードで会員登録を行う
        final NCMBUser ncmbUser = (NCMBUser) user.toNCMBObject();
        ncmbUser.signUpInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //成功
                    try{
                        //ニックネームを登録するためにログイン
                        ncmbUser.loginInBackground(user.getUserName(), user.getPassword(), new LoginCallback() {
                                    @Override
                                    public void done(NCMBUser ncmbUser, NCMBException e) {
                                        if (e == null) {
                                            //ニックネームを登録して保存
                                            User newUser = new User(ncmbUser);
                                            newUser.setNickName(user.getNickName());
                                            newUser.toNCMBObject().saveInBackground(new DoneCallback() {
                                                @Override
                                                public void done(NCMBException e) {
                                                    if (e == null) {
                                                        signinListener.success(user);
                                                    } else {
                                                        //ニックネーム保存に失敗
                                                        signinListener.error(e);
                                                    }
                                                }
                                            });
                                        }else{
                                            //エラー
                                            signinListener.error(e);
                                        }
                                    }
                                }
                        );
                    }catch (NCMBException ex) {
                        //ex.printStackTrace();
                        signinListener.error(ex);
                    }
                } else {
                    //会員登録時にエラーが発生した場合の処理
                    signinListener.error(e);
                }
            }
        });
    }

    /** ログイン処理
     *
     * @param user
     * @param loginListener
     */
    public void login(final User user,final LoginListener loginListener){
        try {
            //ユーザ名とパスワードを指定してログインを実行
            NCMBUser.loginInBackground(user.getUserName(), user.getPassword(), new LoginCallback() {
                @Override
                public void done(NCMBUser user, NCMBException e) {
                    if (e == null) {
                        //成功
                        loginListener.success(new User(user));
                    } else{
                        //エラー時の処理
                        loginListener.error(e);
                    }
                }
            });
        }catch (NCMBException e) {
            //e.printStackTrace();
            loginListener.error(e);
        }
    }

    /** ログアウト処理
     *
     * @param logoutListener
     */
    public void logout(final LogoutListener logoutListener){
        NCMBUser.logoutInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //成功
                    logoutListener.success();
                } else{
                    //エラー時の処理
                    logoutListener.error(e);
                }
            }
        });
    }

    /** 認証済みユーザーを取得を取得する
     *
     * @return 認証済みユーザー
     */
    public User getCurrentUser(){
        //認証済みユーザーを取得してユーザー名の登録常態を確認する
        NCMBUser currentUser = NCMBUser.getCurrentUser();
        if((currentUser == null)||(currentUser.getUserName() == null)){
            //ログイン済みユーザーが取得できなかった
            return null;
        }
        return new User(currentUser);
    }
}
