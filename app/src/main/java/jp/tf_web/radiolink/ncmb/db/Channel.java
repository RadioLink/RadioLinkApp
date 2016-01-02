package jp.tf_web.radiolink.ncmb.db;

import com.nifty.cloud.mb.core.NCMBFile;
import com.nifty.cloud.mb.core.NCMBObject;
import com.nifty.cloud.mb.core.NCMBUser;

/** チャンネル
 *
 * Created by furukawanobuyuki on 2016/01/02.
 */
public class Channel implements NCMBObjectInterface {

    //チャンネル作成ユーザー
    private NCMBUser user;

    //チャンネル アイコン
    private NCMBFile icon;

    //チャンネルコード
    public String channelCode;

    /** コンストラクタ
     *
     * @param channelCode チャンネルコード
     */
    public Channel(final NCMBUser user,final String channelCode){
        this.user = user;
        this.channelCode = channelCode;
    }

    /** チャンネル アイコンを設定
     *
     * @param icon
     */
    public void setIcon(NCMBFile icon){
        this.icon = icon;
    }

    /** 保存するために NCMBObject に変換
     *
     * @return
     */
    @Override
    public NCMBObject toNCMBObject(){
        NCMBObject obj = new NCMBObject("Channel");
        obj.put("user", this.user);
        obj.put("channelCode", this.channelCode);
        return obj;
    }
}
