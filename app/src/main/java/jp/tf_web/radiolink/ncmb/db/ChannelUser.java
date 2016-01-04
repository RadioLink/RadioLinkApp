package jp.tf_web.radiolink.ncmb.db;

import android.util.Log;

import com.nifty.cloud.mb.core.FetchCallback;
import com.nifty.cloud.mb.core.NCMBAcl;
import com.nifty.cloud.mb.core.NCMBBase;
import com.nifty.cloud.mb.core.NCMBException;
import com.nifty.cloud.mb.core.NCMBObject;
import java.net.InetSocketAddress;

/** チャンネルユーザー
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class ChannelUser implements NCMBObjectInterface {
    private static String TAG = "ChannelUser";

    //オブジェクト名
    public static String OBJ_NAME = "ChannelUser";

    //キー名 チャンネル
    public static String KEY_CHANNEL = "channel";

    //キー ユーザー
    public static String KEY_USER = "user";

    //キー パブリックアドレス
    public static String KEY_PUBLIC_ADDRESS = "publicAddress";

    //キー パブリックポート
    public static String KEY_PUBLIC_PORT = "publicPort";

    //キー ローカルアドレス
    public static String KEY_LOCAL_ADDRESS = "localAddress";

    //キー ローカルポート
    public static String KEY_LOCAL_PORT = "localPort";

    private NCMBObject obj;

    //チャンネル
    public Channel channel;

    //チャンネル作成ユーザー
    private User user;

    //STUN サーバから取得したしたグローバルIP,ポート
    public InetSocketAddress publicSocketAddress;

    //受信につかうNAT内のIP,ポート
    public InetSocketAddress localSocketAddress;

    /** コンストラクタ
     *
     * @param user ユーザー
     * @param publicSocketAddress STUN サーバから取得したしたグローバルIP,ポート
     * @param localSocketAddress 受信につかうNAT内のIP,ポート
     */
    public ChannelUser(final Channel channel,final User user,final InetSocketAddress publicSocketAddress,final InetSocketAddress localSocketAddress){
        this.channel = new Channel(channel);
        this.user = new User(user);
        this.publicSocketAddress = publicSocketAddress;
        this.localSocketAddress = localSocketAddress;
    }

    /** コンストラクタ
     *
     * @param src
     */
    public ChannelUser(final NCMBObject src){
        this.obj = src;
        this.obj.setObjectId( src.getObjectId() );
        try {
            this.obj.fetch();
        } catch (NCMBException e) {
            e.printStackTrace();
        }
        this.channel = new Channel(this.obj.getJSONObject(KEY_CHANNEL));
        this.user = new User(this.obj.getJSONObject(KEY_USER));

        String publicAddr = src.getString(KEY_PUBLIC_ADDRESS);
        int publicPort = src.getInt(KEY_PUBLIC_PORT);
        this.publicSocketAddress = new InetSocketAddress(publicAddr,publicPort);

        String localAddr = src.getString(KEY_LOCAL_ADDRESS);
        int localPort = src.getInt(KEY_LOCAL_PORT);
        this.localSocketAddress = new InetSocketAddress(localAddr,localPort);
    }

    /** コンストラクタ
     *
     * @param src
     */
    public ChannelUser(final ChannelUser src){
        copy(src);
    }

    /** コピー
     *
     * @param src
     */
    public void copy(final ChannelUser src){
        this.channel = src.channel;
        this.user = new User(src.user);
        this.publicSocketAddress = src.publicSocketAddress;
        this.localSocketAddress = src.localSocketAddress;
    }

    /** ユーザー取得
     *
     * @return
     */
    public User getUser(){
        return this.user;
    }

    @Override
    public NCMBObject toNCMBObject() {
        if(obj == null) obj = new NCMBObject(OBJ_NAME);

        obj.put(KEY_CHANNEL, this.channel.toNCMBObject());
        obj.put(KEY_USER, this.user.toNCMBObject());
        obj.put(KEY_PUBLIC_ADDRESS, this.publicSocketAddress.getAddress().getHostAddress());
        obj.put(KEY_PUBLIC_PORT,this.publicSocketAddress.getPort());
        obj.put(KEY_LOCAL_ADDRESS, this.localSocketAddress.getAddress().getHostAddress());
        obj.put(KEY_LOCAL_PORT,this.localSocketAddress.getPort());
        return obj;
    }
}
