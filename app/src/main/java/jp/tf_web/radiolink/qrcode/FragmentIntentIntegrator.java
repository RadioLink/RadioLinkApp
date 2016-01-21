package jp.tf_web.radiolink.qrcode;

import android.app.Fragment;
import android.content.Intent;

import com.google.zxing.integration.android.IntentIntegrator;

/**
 * Created by furukawanobuyuki on 2016/01/22.
 */
public class FragmentIntentIntegrator extends IntentIntegrator {
    private final Fragment fragment;

    public FragmentIntentIntegrator(Fragment fragment) {
        super(fragment.getActivity());
        this.fragment = fragment;
    }

    @Override
    protected void startActivityForResult(Intent intent, int code) {
        fragment.startActivityForResult(intent, code);
    }
}
