package jp.tf_web.radiolink.ncmb;

import android.content.Context;
import android.util.Log;

import com.nifty.cloud.mb.core.DoneCallback;
import com.nifty.cloud.mb.core.FetchFileCallback;
import com.nifty.cloud.mb.core.FindCallback;
import com.nifty.cloud.mb.core.LoginCallback;
import com.nifty.cloud.mb.core.NCMB;
import com.nifty.cloud.mb.core.NCMBAcl;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBFile;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBQuery;
import com.nifty.cloud.mb.core.NCMBUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.ncmb.db.ChannelUser;
import jp.tf_web.radiolink.ncmb.db.User;
import jp.tf_web.radiolink.ncmb.listener.CreateChannelListener;
import jp.tf_web.radiolink.ncmb.listener.DeleteChannelListener;
import jp.tf_web.radiolink.ncmb.listener.GetChannelIconImageListener;
import jp.tf_web.radiolink.ncmb.listener.GetChannelListListener;
import jp.tf_web.radiolink.ncmb.listener.GetChannelUserListListener;
import jp.tf_web.radiolink.ncmb.listener.LoginListener;
import jp.tf_web.radiolink.ncmb.listener.LogoutListener;
import jp.tf_web.radiolink.ncmb.listener.SetChannelIconImageListener;
import jp.tf_web.radiolink.ncmb.listener.SigninListener;
import jp.tf_web.radiolink.ncmb.listener.UpdateChannelUserListener;

/** APIサーバとの処理の実装
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class NCMBUtil {
    private static String TAG = "NCMBUtil";

    //チャンネルアイコンの拡張子
    private static String CHANNEL_ICON_IMAGE_EXTENSION = "jpg";

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
    public void signin(final User user,final SigninListener listener){
        //設定したユーザ名とパスワードで会員登録を行う
        final NCMBUser ncmbUser = (NCMBUser) user.toNCMBObject();
        ncmbUser.signUpInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //成功
                    try {
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
                                                        listener.success(user);
                                                    } else {
                                                        //ニックネーム保存に失敗
                                                        listener.error(e);
                                                    }
                                                }
                                            });
                                        } else {
                                            //エラー
                                            listener.error(e);
                                        }
                                    }
                                }
                        );
                    } catch (NCMBException ex) {
                        //ex.printStackTrace();
                        listener.error(ex);
                    }
                } else {
                    //会員登録時にエラーが発生した場合の処理
                    listener.error(e);
                }
            }
        });
    }

    /** ログイン処理
     *
     * @param user
     * @param listener
     */
    public void login(final User user,final LoginListener listener){
        try {
            //ユーザ名とパスワードを指定してログインを実行
            NCMBUser.loginInBackground(user.getUserName(), user.getPassword(), new LoginCallback() {
                @Override
                public void done(NCMBUser user, NCMBException e) {
                    if (e == null) {
                        //成功
                        listener.success(new User(user));
                    } else{
                        //エラー時の処理
                        listener.error(e);
                    }
                }
            });
        }catch (NCMBException e) {
            //e.printStackTrace();
            listener.error(e);
        }
    }

    /** ログアウト処理
     *
     * @param listener
     */
    public void logout(final LogoutListener listener){
        NCMBUser.logoutInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //成功
                    listener.success();
                } else {
                    //エラー時の処理
                    listener.error(e);
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

    /** チャンネル新規作成
     *
     * @param channelCode
     * @param listener
     */
    public void createChannel(final String channelCode,final CreateChannelListener listener){
        //ログイン中のユーザを取得
        User currentUser = getCurrentUser();
        if(currentUser == null){
            //エラー
            listener.error(new NCMBException(NCMBException.GENERIC_ERROR,"currentUser is null"));
            return;
        }
        //検索して無かったら作成
        final Channel channel = new Channel(currentUser,channelCode);
        getChannelList(channelCode, new GetChannelListListener() {
            @Override
            public void success(List<Channel> channels) {
                if (channels.size() == 0) {
                    //新規作成
                    channel.toNCMBObject().saveInBackground(new DoneCallback() {
                        @Override
                        public void done(NCMBException e) {
                            if (e == null) {
                                //成功
                                listener.success(channel);
                            } else {
                                //エラー時の処理
                                listener.error(e);
                            }
                        }
                    });
                } else {
                    //既にあったので登録できないエラー
                    listener.error(new NCMBException(NCMBException.GENERIC_ERROR, "channels.size() is not 0"));
                }
            }

            @Override
            public void error(NCMBException e) {
                //エラー時の処理
                listener.error(e);
            }
        });
    }

    /** チャンネル削除
     *
     * @param channelCode
     * @param listener
     */
    public void deleteChannel(final String channelCode,final DeleteChannelListener listener){
        //ログイン中のユーザを取得
        User currentUser = getCurrentUser();
        if(currentUser == null){
            //エラー
            listener.error(new NCMBException(NCMBException.GENERIC_ERROR,"currentUser is null"));
            return;
        }

        //検索
        NCMBQuery<NCMBObject> query = new NCMBQuery<>(Channel.OBJ_NAME);

        //他人が作ったチャンネルは削除できない
        NCMBQuery<NCMBObject> subQuery = new NCMBQuery<>(User.OBJ_NAME);
        subQuery.whereEqualTo(User.KEY_USER_NAME,currentUser.getUserName());
        query.whereMatchesQuery(Channel.KEY_USER, subQuery);

        query.whereEqualTo(Channel.KEY_CHANNEL_CODE, channelCode);
        query.findInBackground(new FindCallback<NCMBObject>() {
            @Override
            public void done(List<NCMBObject> list, NCMBException e) {
                if (e == null) {
                    //見つかった場合
                    if (list.size() == 0) {
                        //リストサイズが0だった場合
                        listener.error(new NCMBException(NCMBException.GENERIC_ERROR, "list size is 0"));
                    } else {
                        list.get(0).deleteObjectInBackground(new DoneCallback() {
                            @Override
                            public void done(NCMBException e) {
                                if (e == null) {
                                    //成功
                                    listener.success();
                                } else {
                                    //失敗
                                    listener.error(e);
                                }
                            }
                        });
                    }
                } else {
                    //失敗
                    listener.error(e);
                }
            }
        });
    }

    /** チャンネルユーザー一覧を更新
     *
     * @param channel
     * @param listener
     */
    public void updateChannelUserList(final Channel channel,final UpdateChannelUserListener listener){
        //チャンネルユーザー一覧を保存
        for(ChannelUser cu:channel.getChannelUserList()){
            try {
                Log.d(TAG,"cu "+cu.channel+" user:"+cu.channel.getUser());
                cu.toNCMBObject().save();
            } catch (NCMBException e) {
                e.printStackTrace();
            }
        }
        listener.success(channel);
    }

    /** チャンネルユーザー一覧を取得
     *
     * @param channel
     * @param listener
     */
    public void getChannelUserList(final Channel channel,final GetChannelUserListListener listener){
        //検索
        NCMBQuery<NCMBObject> query = new NCMBQuery<>(ChannelUser.OBJ_NAME);

        NCMBQuery<NCMBObject> subQuery = new NCMBQuery<>(Channel.OBJ_NAME);
        subQuery.whereEqualTo(Channel.KEY_CHANNEL_CODE,channel.getChannelCode());
        query.whereMatchesQuery(ChannelUser.KEY_CHANNEL, subQuery);
        query.findInBackground(new FindCallback<NCMBObject>() {

            @Override
            public void done(List<NCMBObject> list, NCMBException e) {
                if(e == null){
                    //成功
                    //チャンネルユーザーを更新
                    List<ChannelUser> channelUsers = new ArrayList<ChannelUser>();
                    for(NCMBObject c:list){
                        channelUsers.add(new ChannelUser(c));
                    }
                    channel.setChannelUserList(channelUsers);
                    listener.success(channel);
                }else{
                    //失敗
                    listener.error(e);
                }
            }
        });
    }

    /** チャンネルアイコンのファイル名生成
     *
     * @param channel
     * @param extension
     * @return
     */
    private String creteChannelIconName(final Channel channel,final String extension){
        return channel.getChannelCode()+"."+extension.toLowerCase();
    }

    /** チャンネルアイコン設定
     *
     * @param channel アイコンを設定するチャンネル
     * @param jpg 画像のbyte配列
     * @param listener リスナー
     */
    public void saveChannelIcon(final Channel channel,final byte[] jpg,final SetChannelIconImageListener listener){
        //ログイン中のユーザを取得
        User currentUser = getCurrentUser();
        if(currentUser == null){
            //エラー
            listener.error(new NCMBException(NCMBException.GENERIC_ERROR,"currentUser is null"));
            return;
        }

        //他人が作ったチャンネルはアイコン登録できない
        if(channel.getUser().getUserName().equals(currentUser.getUserName()) == false){
            listener.error(new NCMBException(NCMBException.GENERIC_ERROR,"not an owner of a channel."));
            return;
        }

        //アイコンイメージを保存する
        String filename = creteChannelIconName(channel, CHANNEL_ICON_IMAGE_EXTENSION);
        //パーミッションを設定
        NCMBAcl acl = new NCMBAcl();
        acl.setPublicReadAccess(true);
        acl.setPublicWriteAccess(true);

        final NCMBFile file = new NCMBFile(filename, jpg, acl);
        file.saveInBackground(new DoneCallback() {
            @Override
            public void done(NCMBException e) {
                if (e == null) {
                    //成功
                    channel.setIcon(file);
                    listener.success(channel);
                } else {
                    //失敗
                    listener.error(e);
                }
            }
        });

    }

    /** チャンネル画像を取得
     *
     * @param channel
     * @param listener
     */
    public void getChannelIcon(final Channel channel,final GetChannelIconImageListener listener){
        String filename = creteChannelIconName(channel,CHANNEL_ICON_IMAGE_EXTENSION);
        final NCMBFile file = new NCMBFile(filename);
        file.fetchInBackground(new FetchFileCallback() {
            @Override
            public void done(byte[] data, NCMBException e) {
                if (e == null) {
                    //成功
                    channel.setIcon(file);
                    listener.success(channel);
                } else {
                    //失敗
                    listener.error(e);
                }
            }
        });
    }

    /** チャンネル一覧を取得する
     *
     * @param listener
     */
    public void getChannelList(final GetChannelListListener listener){
        getChannelList(null,listener);
    }

    /** チャンネル一覧を取得する
     *
     * @param channelCode 検索対象のチャンネルコード
     * @param listener listener
     */
    public void getChannelList(final String channelCode,final GetChannelListListener listener){
        //検索
        NCMBQuery<NCMBObject> query = new NCMBQuery<>(Channel.OBJ_NAME);
        if(channelCode != null){
            Log.d(TAG, "getChannelList channelCode " + channelCode);
            query.whereContainedIn(Channel.KEY_CHANNEL_CODE, Arrays.asList(channelCode));
        }
        query.findInBackground(new FindCallback<NCMBObject>() {
            @Override
            public void done(final List<NCMBObject> list, NCMBException e) {
                if (e == null) {
                    //見つかった場合
                    if (list.size() == 0) {
                        //リストサイズが0だった場合 0 件を返す
                        final List<Channel> channels = new ArrayList<Channel>();
                        listener.success(channels);
                    } else {
                        //成功
                        final List<Channel> channels = new ArrayList<Channel>();
                        for(NCMBObject obj:list){
                            Channel c = new Channel(obj);
                            channels.add(c);
                        }
                        listener.success(channels);
                    }
                } else {
                    //失敗
                    listener.error(e);
                }
            }
        });
    }
}
