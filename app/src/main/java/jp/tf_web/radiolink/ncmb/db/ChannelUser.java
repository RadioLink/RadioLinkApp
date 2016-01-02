package jp.tf_web.radiolink.ncmb.db;

import android.util.Log;

import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

import org.json.JSONObject;

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

    //キー ニックネーム
    public static String KEY_NICK_NAME = "nickName";

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
    private Channel channel;

    //ニックネーム
    public String nickName;

    //STUN サーバから取得したしたグローバルIP,ポート
    public InetSocketAddress publicSocketAddress;

    //受信につかうNAT内のIP,ポート
    public InetSocketAddress localSocketAddress;

    /** コンストラクタ
     *
     * @param nickName ニックネーム
     * @param publicSocketAddress STUN サーバから取得したしたグローバルIP,ポート
     * @param localSocketAddress 受信につかうNAT内のIP,ポート
     */
    public ChannelUser(final Channel channel,final String nickName,final InetSocketAddress publicSocketAddress,final InetSocketAddress localSocketAddress){
        this.channel = channel;
        this.nickName = nickName;
        this.publicSocketAddress = publicSocketAddress;
        this.localSocketAddress = localSocketAddress;
    }

    /** コンストラクタ
     *
     * @param src
     */
    public ChannelUser(final NCMBObject src){
        this.obj = src;

        this.channel = new Channel(src.getJSONObject(KEY_CHANNEL));
        this.nickName = src.getString(KEY_NICK_NAME);

        String publicAddr = src.getString(KEY_PUBLIC_ADDRESS);
        int publicPort = src.getInt(KEY_PUBLIC_PORT);
        this.publicSocketAddress = new InetSocketAddress(publicAddr,publicPort);

        String localAddr = src.getString(KEY_PUBLIC_ADDRESS);
        int localPort = src.getInt(KEY_PUBLIC_PORT);
        this.localSocketAddress = new InetSocketAddress(localAddr,localPort);
    }

    @Override
    public NCMBObject toNCMBObject() {
        if(obj == null) obj = new NCMBObject(OBJ_NAME);
        obj.put(KEY_CHANNEL, this.channel.toNCMBObject());
        obj.put(KEY_NICK_NAME, this.nickName);
        obj.put(KEY_PUBLIC_ADDRESS, this.publicSocketAddress.getAddress().getHostAddress());
        obj.put(KEY_PUBLIC_PORT,this.publicSocketAddress.getPort());
        obj.put(KEY_LOCAL_ADDRESS, this.localSocketAddress.getAddress().getHostAddress());
        obj.put(KEY_LOCAL_PORT,this.localSocketAddress.getPort());
        return obj;
    }
}
