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
public class FragmentBarCode extends MainActivity.MyFragment {
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        int width = displayMetrics.widthPixels / 6 * 5;
        int height = displayMetrics.heightPixels / 4;

        Bitmap bitmap = createBarCode(displayMetrics, getContentId(), BarcodeFormat.CODE_128, width, height);

        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        imageView.setMaxWidth(width);
        imageView.setMaxHeight(height);
        imageView.setImageBitmap(bitmap);

        ((TextView) view.findViewById(R.id.textView)).setText(getContentId());
    }

}
