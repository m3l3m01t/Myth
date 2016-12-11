package com.visualstudio.m3l3m01t.myth;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;

/**
 * Created by jiff.shen on 16/12/11.
 */
public class FragmentBarCode extends MainActivity.MyFragment {
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);

        Bitmap bitmap = createBarCode(mContentId, BarcodeFormat.CODABAR, imageView.getWidth(), imageView.getHeight());

        imageView.setImageBitmap(bitmap);

        ((TextView) view.findViewById(R.id.textView)).setText(mContentId);
    }
}
