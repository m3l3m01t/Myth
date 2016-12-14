package com.visualstudio.m3l3m01t.myth;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;

/**
 * Created by jiff.shen on 16/12/11.
 */
public class FragmentQRCode extends MainActivity.MyFragment {
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        int width = displayMetrics.widthPixels / 6 * 5;
        int height = displayMetrics.heightPixels / 6 * 5;

        Bitmap bitmap = createBarCode(displayMetrics, getContentId(), BarcodeFormat.QR_CODE, width, height);

        imageView.setMaxWidth(width);
        imageView.setMaxHeight(height);
        imageView.setImageBitmap(bitmap);

        ((TextView) view.findViewById(R.id.textView)).setText(getContentId());
    }
}
