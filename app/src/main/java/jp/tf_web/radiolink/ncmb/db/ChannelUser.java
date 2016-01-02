package jp.tf_web.radiolink.ncmb.db;

import com.nifty.cloud.mb.core.NCMBObject;

import java.net.InetSocketAddress;

/** チャンネルユーザー
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class ChannelUser implements NCMBObjectInterface {
    //チャンネル
    public Channel channel;

    //ニックネーム
    public String nickName;

    //STUN サーバから取得したしたグローバルIP,ポート
    public InetSocketAddress publicSocketAddress;

    //受信につかうNAT内のIP,ポート
    public InetSocketAddress localSocketAddress;

    /** コンストラクタ
     *
     * @param channel 通話対象のチャンネル
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

    @Override
    public NCMBObject toNCMBObject() {
        return null;
    }
}
