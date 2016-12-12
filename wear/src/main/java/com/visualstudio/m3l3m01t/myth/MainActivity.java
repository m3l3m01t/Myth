package com.visualstudio.m3l3m01t.myth;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;


public class MainActivity extends WearableActivity {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final String KEY_ROW = "KEY_ROW";
    private static final String KEY_COL = "KEY_COL";
    public static String mContentId = "29294117388747849490";
    private BoxInsetLayout mContainerView;
    private TextView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);

        GridViewPager pager = (GridViewPager) findViewById(R.id.pager);
        pager.setAdapter(new MyPagerAdapter(getFragmentManager()));
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
//            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
//            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }

    public static class MyFragment extends Fragment {
        private static Vector<Pair<Integer, Class<? extends MyFragment>>> mLayouts = new Vector<>();

        static {
            mLayouts.addElement(new Pair(R.layout.fragment_qrcode, FragmentQRCode.class));
            mLayouts.addElement(new Pair(R.layout.fragment_barcode, FragmentBarCode.class));
//            mLayouts.addElement(new Pair(R.layout.fragment_content, FragmentContent.class));
        }

        protected int mRow;
        protected int mCol;

        public MyFragment() {

        }

        public static Fragment create(Bundle bundle, int row, int col) {
            if (col >= mLayouts.size()) {
                return null;
            }

            Class<? extends MyFragment> klazz = mLayouts.get(col).second;

            MyFragment fragment = null;
            try {
                fragment = klazz.getConstructor().newInstance();

                fragment.setArguments(bundle);
                return fragment;
            } catch (java.lang.InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            return null;
        }

        public static Bitmap toBitmap(DisplayMetrics metrics, BitMatrix matrix) {
            int width = matrix.getWidth();
            int height = matrix.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(metrics,
                    width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) == true ? Color.BLACK : Color.WHITE);
                }
            }


            return bitmap;
        }

        protected String getContentId() {
            return mContentId;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }

//        abstract public void onViewCreated(View view, Bundle savedInstanceState);

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);

            Bundle bundle = getArguments();
            mRow = bundle.getInt(KEY_ROW);
            mCol = bundle.getInt(KEY_COL);

            if (mCol < mLayouts.size()) {
                return inflater.inflate(mLayouts.get(mCol).first, null);
            }
            return null;
        }

        protected Bitmap createBarCode(DisplayMetrics displayMetrics, String s, BarcodeFormat format, int width, int
                height) {
            MultiFormatWriter writer = new MultiFormatWriter();
            try {
                BitMatrix bitMatrix = writer.encode(s, format, width, height);


                return toBitmap(displayMetrics, bitMatrix);

            } catch (WriterException e) {
                e.printStackTrace();

                return null;
            }
        }
    }

    private class MyPagerAdapter extends FragmentGridPagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getFragment(int row, int col) {
            Bundle bundle = new Bundle();

            bundle.putInt(KEY_ROW, row);
            bundle.putInt(KEY_COL, col);
            return MyFragment.create(bundle, row, col);
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int row) {
            return 2;
        }
    }
}
