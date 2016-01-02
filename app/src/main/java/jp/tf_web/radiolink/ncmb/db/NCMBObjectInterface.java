package jp.tf_web.radiolink.ncmb.db;

import com.nifty.cloud.mb.core.NCMBObject;

/**
 * Created by furukawanobuyuki on 2016/01/02.
 */
public interface NCMBObjectInterface {
    /** NCMBObject に変換
     *
     * @return 変換のNCMBObject
     */
    NCMBObject toNCMBObject();
}
