package jp.tf_web.radiolink.net;

import android.os.AsyncTask;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by furukawanobuyuki on 2015/12/18.
 */
public class NetWorkUtil {

    private static String TAG = "NetWorkUtil";

    /** 端末に割り振れている IPv4 を非同期で取得する
     *
     * @param listener
     */
    public static void getLocalIpv4Address(final GetLocalIpv4AddressListener listener){
        AsyncTask task = new AsyncTask<Object, Void, String>(){
            @Override
            protected String doInBackground(Object... params) {
                final String localAddress = getLocalIpv4Address();
                return localAddress;
            }

            @Override
            protected void onPostExecute(String result) {
                listener.onResult(result);
            }
        };
        task.execute();
    }

    /** 端末に割り振れている IPv4 を取得する
     *
     * @return
     */
    public static String getLocalIpv4Address(){
        String result = "127.0.0.1";
        try{
            Enumeration<NetworkInterface> networkInterfaceEnum = NetworkInterface.getNetworkInterfaces();
            while(networkInterfaceEnum.hasMoreElements()){
                NetworkInterface network = networkInterfaceEnum.nextElement();

                if((network.isLoopback() == true) || (network.isUp() == false) || (network.getName().equals("wlan0") == false)){
                    continue;
                }
                Log.i(TAG, "Network:" + network.getName());

                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while(addresses.hasMoreElements()){
                    InetAddress inetAddress = addresses.nextElement();
                    String address = inetAddress.getHostAddress();
                    boolean ip4type = (inetAddress instanceof Inet4Address);
                    if(ip4type){
                        result = address;
                    }
                    /*
                    else {
                        boolean ip6type = (inetAddress instanceof Inet6Address);
                        if (ip6type) {
                            //ip6
                            address = address.substring(0, address.indexOf('%'));
                        }
                    }
                    */
                    Log.i(TAG, ((ip4type)?"ip4":"ip6")+" addr:"+address);
                }
            }
        }
        catch (SocketException ex){
            Log.e(TAG, ex.toString());
        }
        return result;
    }

    public interface GetLocalIpv4AddressListener {
        /** ローカルIPアドレスの取得結果を通知
         *
         * @param address
         */
        void onResult(String address);
    };
}
