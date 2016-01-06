package jp.tf_web.radiolink.net.protocol;

import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jp.tf_web.radiolink.net.protocol.packet.Header;
import jp.tf_web.radiolink.net.protocol.packet.Packet;
import jp.tf_web.radiolink.net.protocol.packet.Payload;
import jp.tf_web.radiolink.util.ByteArrayUtil;

/**
 * Created by furukawanobuyuki on 2016/01/04.
 */
public class PacketUtil {
    private static String TAG = "PacketUtil";

    //シーケンス番号
    private long seq = 0;

    public void test(){

        seq++;
        long timeStamp = System.currentTimeMillis();
        String ssrc = UUID.randomUUID().toString().replaceAll("-","").trim();
        Log.d(TAG, "ssrc:" + ssrc);

        Header header = new Header(ssrc,seq,timeStamp);


        List<Payload > payload = new ArrayList<>();
        {
            final byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09};
            payload.add(new Payload((short) data.length, data));
        }
        {
            final byte[] data = {0x09,0x08,0x07,0x06,0x05,0x04,0x03,0x02,0x01,0x00};
            payload.add(new Payload((short) data.length, data));
        }
        Packet packet = new Packet(header,payload);
        byte[] dst = packet.toByteArray();
        Log.d(TAG, "packet size:"+dst.length+" "+ ByteArrayUtil.toHexString(dst) );

        try {
            Packet packet2 = new Packet(dst);
            byte[] tmp = packet2.parse();

            Header header2 = packet2.getHeader();
            Log.d(TAG, "ssrc:" + header2.getSsrc());

            List<Payload> payload2 = packet2.getPayload();
            Log.d(TAG, "payload2 size:" + payload2.size());
            for(Payload p:payload2) {
                Log.d(TAG, "p size:" + p.getSize() + " " + ByteArrayUtil.toHexString(p.getData()));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
