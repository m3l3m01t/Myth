package com.visualstudio.m3l3m01t.myth;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.util.ArraySet;
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
import java.util.*;


public class MainActivity extends WearableActivity {

    static final String KEY_ROW = "KEY_ROW";
    static final String KEY_COL = "KEY_COL";
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private final static String ACTION_REMOVE = "com.github.m3l3m01t.myth.remove";
    private final static String ACTION_ADD = "com.github.m3l3m01t.myth.add";
    private static Set<String> mIdList = new ArraySet<String>();
    private static Vector<Pair<Integer, Class<? extends MyFragment>>> mLayouts = new Vector<>();

    static {
        mLayouts.addElement(new Pair(R.layout.fragment_qrcode, FragmentQRCode.class));
        mLayouts.addElement(new Pair(R.layout.fragment_barcode, FragmentBarCode.class));
//            mLayouts.addElement(new Pair(R.layout.fragment_content, FragmentContent.class));
    }

    GridViewPager mPager;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.arg1 == 1) {
                add(msg.getData().getString("ID"));
            } else if (msg.arg1 == 2) {
                remove(msg.getData().getString("ID"));
            }
        }
    };
    //    public static String mContentId = "29294117388747849490";
    private BoxInsetLayout mContainerView;
    private TextView mClockView;

    public static Fragment create(Bundle bundle, int row, int col) {
        if (row > mIdList.size()) {
            return null;
        }

        Class<? extends MyFragment> klazz;

        if (row == mIdList.size()) {
            klazz = FragmentAction.class;
        } else {

            if (col >= mLayouts.size()) {
                return null;
            }

            klazz = mLayouts.get(col).second;
        }


        try {
            MyFragment fragment;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);

        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new MyPagerAdapter(getFragmentManager()));
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
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mClockView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preference = getPreferences(MODE_PRIVATE);

        mIdList = preference.getStringSet("ID_SET", mIdList);

//        IntentFilter filter = new IntentFilter(ACTION_REMOVE);
//
//        filter.addAction(ACTION_ADD);
//        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);


        SharedPreferences preference = getPreferences(MODE_PRIVATE);
        preference.edit().putStringSet("ID_SET", mIdList).commit();
    }

/*    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_ADD)) {
                String item = intent.getStringExtra("ITEM");

                add(item);
            } else if (intent.getAction().equals(ACTION_REMOVE)) {
                String item = intent.getStringExtra("ITEM");

                remove(item);
            }
        }
    };*/

    public void sendMessage(int arg1, String id) {
        Message message = mHandler.obtainMessage();

        message.arg1 = arg1;

        Bundle bundle = new Bundle();

        bundle.putString("ID", id);
        message.setData(bundle);
        message.sendToTarget();
    }

    public void add(String spokenText) {
        synchronized (mIdList) {
            mIdList.add(spokenText);
        }

        mPager.getAdapter().notifyDataSetChanged();
    }

    public void remove(String spokenText) {
        synchronized (mIdList) {
            mIdList.remove(spokenText);
        }
        mPager.getAdapter().notifyDataSetChanged();
    }

    public static class MyFragment extends Fragment {
        protected int mRow;
        protected int mCol;

        public MyFragment() {

        }

        public Bitmap toBitmap(DisplayMetrics metrics, BitMatrix matrix) {
            int width = matrix.getWidth();
            int height = matrix.getHeight();

            Bitmap bitmap = Bitmap.createBitmap(metrics,
                    width, height, Bitmap.Config.ARGB_8888);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        }

        protected String getContentId() {
            return mIdList.toArray(new String[0])[mRow];
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

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
            } catch (IllegalArgumentException e) {
                e.printStackTrace();

                ((MainActivity) getActivity()).sendMessage(2, s);

                return null;
            }
        }
    }

    /**
     * Created by jiff.shen on 16/12/13.
     */
    public static class FragmentAction extends MyFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle bundle = getArguments();
            mRow = bundle.getInt(KEY_ROW);
            mCol = bundle.getInt(KEY_COL);

            return inflater.inflate(R.layout.fragment_action, null);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            View imageView = view.findViewById(R.id.imageButton);
            imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    displaySpeechRecognizer();
                }
            });
        }

        // Create an intent that can start the Speech Recognizer activity
        void displaySpeechRecognizer() {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            // Start the activity, the intent will be populated with the speech text
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_CANCELED)
                return;
            if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                String s = results.get(0);

                ((MainActivity) getActivity()).sendMessage(1, s);
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
            return create(bundle, row, col);
        }

        @Override
        public int getRowCount() {
            return mIdList.size() + 1;
        }

        @Override
        public int getColumnCount(int row) {
            if (row < mIdList.size())
                return 2;
            else
                return 1;
        }
    }

}
