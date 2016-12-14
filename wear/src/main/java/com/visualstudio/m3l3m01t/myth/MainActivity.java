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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.visualstudio.m3l3m01t.myth.MainActivity.FragmentBarcode.KEY_BARCODE;
import static com.visualstudio.m3l3m01t.myth.MainActivity.FragmentBarcode.KEY_BITMAP;


public class MainActivity extends WearableActivity {
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private final static String ACTION_REMOVE = "com.github.m3l3m01t.myth.remove";
    private final static String ACTION_ADD = "com.github.m3l3m01t.myth.add";
    protected static HashMap<BarcodeFormat, Integer> mBarcodeDimension = new HashMap<>();

    static {
        mBarcodeDimension.put(BarcodeFormat.QR_CODE, 2);
        mBarcodeDimension.put(BarcodeFormat.AZTEC, 2);
//        mBarcodeDimension.put(BarcodeFormat.DATA_MATRIX, 2);
        mBarcodeDimension.put(BarcodeFormat.PDF_417, 2);

        mBarcodeDimension.put(BarcodeFormat.CODE_128, 1);
        mBarcodeDimension.put(BarcodeFormat.CODE_93, 1);
        mBarcodeDimension.put(BarcodeFormat.CODE_39, 1);
//        mBarcodeDimension.put(BarcodeFormat.CODABAR, 1);
//        mBarcodeDimension.put(BarcodeFormat.ITF, 1);
    }

    GridViewPager mPager;
    private HashMap<String, List<Bitmap>> mBarcodeHash = new HashMap<>();
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
//
//    public static Fragment create(Bundle bundle, int row, int col) {
//        if (row > mIdList.size()) {
//            return null;
//        }
//
//        Class<? extends FragmentBarcode> klazz;
//
//        if (row == mIdList.size()) {
//            klazz = FragmentAction.class;
//        } else {
//
//            if (col >= mLayouts.size()) {
//                return null;
//            }
//
//            klazz = mLayouts.get(col).second;
//        }
//
//
//        try {
//            FragmentBarcode fragment;
//
//            fragment = klazz.getConstructor().newInstance();
//
//            fragment.setArguments(bundle);
//            return fragment;
//        } catch (java.lang.InstantiationException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }

    public static Bitmap bitMatrixtoBitmap(DisplayMetrics metrics, BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(metrics,
                width, height, Bitmap.Config.ARGB_4444);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.LTGRAY);
            }
        }

        return bitmap;
    }

    protected static Bitmap createBarcode(DisplayMetrics displayMetrics, String s, BarcodeFormat format, int width, int
            height) {
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(s, format, width, height);

            return bitMatrixtoBitmap(displayMetrics, bitMatrix);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    protected static List<Bitmap> createBarcodes(DisplayMetrics displayMetrics, String barcode) {
        List<Bitmap> bitmaps = new Vector<>();

        for (Map.Entry<BarcodeFormat, Integer> entry :
                mBarcodeDimension.entrySet()) {
            int width;

            int height;

            if (entry.getValue() == 2) {
                width = (int) (displayMetrics.xdpi * 5 / 6);
                height = width;
            } else {
                width = (int) (displayMetrics.xdpi * 5 / 6);

                height = width / 3;
            }

            Bitmap bitmap = createBarcode(displayMetrics, barcode, entry.getKey(), width, height);
            if (bitmap != null)
                bitmaps.add(bitmap);
        }

        return bitmaps;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preference = getPreferences(MODE_PRIVATE);

        Set<String> barcodes = preference.getStringSet("ID_SET", new HashSet<String>());

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);

        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setAdapter(new MyPagerAdapter(getFragmentManager(), barcodes.toArray(new String[0])));
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


//        IntentFilter filter = new IntentFilter(ACTION_REMOVE);
//
//        filter.addAction(ACTION_ADD);
//        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences preference = getPreferences(MODE_PRIVATE);
        preference.edit().putStringSet("ID_SET", mBarcodeHash.keySet()).commit();
    }

    public void sendMessage(int arg1, String id) {
        Message message = mHandler.obtainMessage();

        message.arg1 = arg1;

        Bundle bundle = new Bundle();

        bundle.putString("ID", id);
        message.setData(bundle);
        message.sendToTarget();
    }

    public void add(String spokenText) {
        if (mBarcodeHash.containsKey(spokenText))
            return;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        List<Bitmap> bitmaps = createBarcodes(displayMetrics, spokenText);
        if (bitmaps.size() > 0) {
            mBarcodeHash.put(spokenText, bitmaps);
            mPager.setAdapter(new MyPagerAdapter(getFragmentManager(), mBarcodeHash.keySet().toArray(new String[0])));
        }
    }

    public void remove(String spokenText) {
        if (!mBarcodeHash.containsKey(spokenText))
            return;
        mBarcodeHash.remove(spokenText);

        mPager.setAdapter(new MyPagerAdapter(getFragmentManager(), mBarcodeHash.keySet().toArray(new String[0])));
    }

    public static class FragmentBarcode extends Fragment {
        protected static final String KEY_BARCODE = "KEY_BARCODE";
        protected static final String KEY_BITMAP = "KEY_BITMAP";
        private String barcode;
        private Bitmap bitmap;

        public FragmentBarcode() {
            super();
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            ImageView imageView = (ImageView) view.findViewById(R.id.imageView);

            imageView.setImageBitmap(bitmap);
            imageView.setMaxHeight(bitmap.getHeight());
            imageView.setMaxWidth(bitmap.getWidth());
            imageView.setScaleType(ImageView.ScaleType.CENTER);

            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ((MainActivity) getActivity()).sendMessage(2, barcode);
                    return true;
                }
            });

            ((TextView) view.findViewById(R.id.textView)).setText(barcode);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);

            Bundle bundle = getArguments();
            barcode = bundle.getString(KEY_BARCODE);
            bitmap = bundle.getParcelable(KEY_BITMAP);

            return inflater.inflate(R.layout.fragment_barcode, null);
        }
    }

    /**
     * Created by jiff.shen on 16/12/13.
     */
    public static class FragmentAction extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            Bundle bundle = getArguments();

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
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
//            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US");
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
                for (String s : results) {
//                String s = results.get(0);

                    ((MainActivity) getActivity()).sendMessage(1, s);
                }
            }
        }
    }

    private class MyPagerAdapter extends FragmentGridPagerAdapter {
        private String[] barcodes;

        public MyPagerAdapter(FragmentManager fm, String[] barcodes) {
            super(fm);
            this.barcodes = barcodes;
        }

        @Override
        public Fragment getFragment(int row, int col) {
            if (row > barcodes.length)
                return null;
            if (row == barcodes.length) {
                if (col > 0)
                    return null;
                return new FragmentAction();
            } else {
                String barcode = barcodes[row];
                if (!mBarcodeHash.containsKey(barcode)) {
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

                    List<Bitmap> bitmaps = createBarcodes(displayMetrics, barcode);
                    if (bitmaps != null && bitmaps.size() > 0)
                        mBarcodeHash.put(barcode, bitmaps);
                    else {
                        return null;
                    }
                }
                List<Bitmap> bitmaps = mBarcodeHash.get(barcode);
                if (col >= bitmaps.size()) {
                    return null;
                } else {
                    Fragment fragment = new FragmentBarcode();
                    Bundle bundle = new Bundle();

                    bundle.putString(KEY_BARCODE, barcode);
                    bundle.putParcelable(KEY_BITMAP, bitmaps.get(col));
                    fragment.setArguments(bundle);

                    return fragment;
                }
            }
        }

        @Override
        public int getRowCount() {
            return barcodes.length + 1;
        }

        @Override
        public int getColumnCount(int row) {
            if (row < barcodes.length) {
                String barcode = barcodes[row];
                if (barcode == null)
                    return 0;
                if (!mBarcodeHash.containsKey(barcode) || mBarcodeHash.get(barcode) == null) {
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
                    List<Bitmap> bitmaps = createBarcodes(displayMetrics, barcode);
                    if (bitmaps != null && bitmaps.size() > 0)
                        mBarcodeHash.put(barcode, bitmaps);
                    else {
                        return 0;
                    }
                }

                return mBarcodeHash.get(barcode).size();
            } else if (row == barcodes.length) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
