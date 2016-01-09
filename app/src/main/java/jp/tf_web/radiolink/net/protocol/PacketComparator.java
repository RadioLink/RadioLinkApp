package jp.tf_web.radiolink.net.protocol;

import java.util.Comparator;

import jp.tf_web.radiolink.net.protocol.packet.Header;
import jp.tf_web.radiolink.net.protocol.packet.Packet;

/** Packetをソートする為のコンパレータクラス
 *
 * Created by furukawanobuyuki on 2016/01/09.
 */
public class PacketComparator implements Comparator<Packet> {
    /** タイムスタンプ,シーケンス番号順で比較する
     *
     * @param lhs
     * @param rhs
     * @return
     */
    @Override
    public int compare(Packet lhs, Packet rhs) {

        Header lHeader = lhs.getHeader();
        Header rHeader = rhs.getHeader();

        //タイムスタンプ比較
        int result = Long.valueOf(lHeader.getTimeStamp()).compareTo(Long.valueOf(rHeader.getTimeStamp()));
        if(result == 0){
            //同じ値だった場合 シーケンス番号順
            result = Long.valueOf(lHeader.getSeq()).compareTo(Long.valueOf(rHeader.getSeq()));
        }

        return result;
    }
}
