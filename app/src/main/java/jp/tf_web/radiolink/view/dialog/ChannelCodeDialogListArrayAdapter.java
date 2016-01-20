package jp.tf_web.radiolink.view.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;

import jp.tf_web.radiolink.R;
import jp.tf_web.radiolink.ncmb.db.Channel;
import jp.tf_web.radiolink.util.BitmapUtil;
import jp.tf_web.radiolink.util.DateUtil;
import jp.tf_web.radiolink.view.CircleImageView;

/**
 * Created by furukawanobuyuki on 2016/01/20.
 */
public class ChannelCodeDialogListArrayAdapter extends ArrayAdapter<Channel> {
    private LayoutInflater layoutInflater;

    /** コンストラクタ
     *
     * @param context
     */
    public ChannelCodeDialogListArrayAdapter(Context context) {
        super(context, 0, new ArrayList<Channel>());
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Channel channel = (Channel)getItem(position);

        if (null == convertView) {
            convertView = layoutInflater.inflate(R.layout.channel_code_dialog_list_item, null);
        }

        //アイコン
        if(channel.getIcon() != null) {
            Bitmap icon = BitmapUtil.byte2bmp(channel.getIcon(),6);
            CircleImageView imgIcon = (CircleImageView) convertView.findViewById(R.id.imgIcon);
            imgIcon.setImageBitmap( icon );
        }

        //チャンネルコード
        TextView txtChannelCode = (TextView)convertView.findViewById(R.id.txtChannelCode);
        txtChannelCode.setText( channel.getChannelCode() );

        //作成日
        TextView txtCreateDate = (TextView)convertView.findViewById(R.id.txtCreateDate);
        Date createDate = channel.toNCMBObject().getCreateDate();
        txtCreateDate.setText(DateUtil.dateFormat(createDate,"yyyy-MM-dd") );

        return convertView;
    }
}
