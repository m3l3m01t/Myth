package com.visualstudio.m3l3m01t.myth;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.visualstudio.m3l3m01t.myth.MainActivity.FragmentBarcode.KEY_BARCODE;
import static com.visualstudio.m3l3m01t.myth.MainActivity.FragmentBarcode.KEY_BITMAP;


public class MainActivity extends WearableActivity {
    private static final int SPEECH_REQUEST_CODE = 0;
    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    protected static HashMap<BarcodeFormat, Integer> mBarcodeDimension = new HashMap<>();

    static {
        mBarcodeDimension.put(BarcodeFormat.QR_CODE, 2);
        mBarcodeDimension.put(BarcodeFormat.AZTEC, 2);
        mBarcodeDimension.put(BarcodeFormat.PDF_417, 2);

        mBarcodeDimension.put(BarcodeFormat.CODE_128, 1);
        mBarcodeDimension.put(BarcodeFormat.CODE_93, 1);
        mBarcodeDimension.put(BarcodeFormat.CODE_39, 1);
    }

    GridViewPager mPager;
    HashMap<String, Integer> mPermissions = new HashMap<>();
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
    private BoxInsetLayout mContainerView;

    private TextView mClockView;

    public static Bitmap bitMatrixtoBitmap(DisplayMetrics metrics, BitMatrix matrix) {
        int[] rect = matrix.getEnclosingRectangle();

        int width = rect[2];
        int height = rect[3];

        Bitmap bitmap = Bitmap.createBitmap(metrics,
                width, height, Bitmap.Config.ARGB_8888);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y,
                        matrix.get(x + rect[0], y + rect[1]) ?
                                Color.BLACK :
                                Color.WHITE);
            }
        }

        return bitmap;
    }

    protected static Bitmap createBarcode(DisplayMetrics displayMetrics, String s, BarcodeFormat format, int width, int
            height) {
        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            Map<EncodeHintType, String> hints = new HashMap<>();

            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix bitMatrix = writer.encode(s, format, width, height, hints);

            return bitMatrixtoBitmap(displayMetrics, bitMatrix);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    protected List<Bitmap> createBarcodes(DisplayMetrics displayMetrics, String barcode) {
        List<Bitmap> bitmaps = new Vector<>();

        for (Map.Entry<BarcodeFormat, Integer> entry :
                mBarcodeDimension.entrySet()) {
            String name = barcode + "_" + entry.getKey().name() + ".bmp";


            int permissionCheck = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE);

            if (PackageManager.PERMISSION_GRANTED == permissionCheck) {
                String url = getPreferences(MODE_PRIVATE).getString(name, null);
                if (url != null) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.parse(url));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (bitmap != null) {
                        bitmaps.add(bitmap);
                        continue;
                    }
                }
            }

            {
                int width, height;

                if (entry.getValue() == 2) {
                    height = width = (int) (displayMetrics.xdpi * 5 / 6);
                } else {
                    width = (int) (displayMetrics.xdpi * 5 / 6);

                    height = width / 3;
                }

                Bitmap bitmap = createBarcode(displayMetrics, barcode, entry.getKey(), width, height);
                if (bitmap != null) {
                    bitmaps.add(bitmap);

                    if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) ==
                            PackageManager
                                    .PERMISSION_GRANTED) {
                        String url = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap,
                                name, barcode);

                        if (url != null) {
                            getPreferences(MODE_PRIVATE).edit().putString(name, url).apply();
                        }
                    }
                }
            }
        }

        return bitmaps;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mBarcodeHash.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preference = getPreferences(MODE_PRIVATE);

        Set<String> barcodes = preference.getStringSet("ID_SET", new HashSet<String>());
        mPager.setAdapter(new MyPagerAdapter(getFragmentManager(), barcodes.toArray(new String[0])));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextView) findViewById(R.id.clock);

        mPager = (GridViewPager) findViewById(R.id.pager);

        ActivityCompat.requestPermissions(
                this, new String[]{READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int i = 0; i < permissions.length; i++) {
            mPermissions.put(permissions[i], grantResults[i]);
        }

        SharedPreferences preference = getPreferences(MODE_PRIVATE);

        Set<String> barcodes = preference.getStringSet("ID_SET", new HashSet<String>());
        mPager.setAdapter(new MyPagerAdapter(getFragmentManager(), barcodes.toArray(new String[0])));
    }

    /*
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
*/

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
            mPager.invalidate();
        } else {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
            mPager.invalidate();
        }
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

            SharedPreferences preference = getPreferences(MODE_PRIVATE);
            preference.edit().putStringSet("ID_SET", mBarcodeHash.keySet()).apply();
        }
    }

    public void remove(String spokenText) {
        if (!mBarcodeHash.containsKey(spokenText))
            return;
        mBarcodeHash.remove(spokenText);

        mPager.setAdapter(new MyPagerAdapter(getFragmentManager(), mBarcodeHash.keySet().toArray(new String[0])));
        SharedPreferences preference = getPreferences(MODE_PRIVATE);
        preference.edit().putStringSet("ID_SET", mBarcodeHash.keySet()).apply();
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

            imageView.setMaxHeight(bitmap.getHeight());
            imageView.setMaxWidth(bitmap.getWidth());
            imageView.setImageBitmap(bitmap);

            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    AlertDialog dialog = new AlertDialog.Builder(getContext())
                            .setTitle("Delete?")
                            .setMessage(barcode)
                            .setNegativeButton(R.string.cancel, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }).setPositiveButton(R.string.confirm, new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((MainActivity) getActivity()).sendMessage(2, barcode);
                                }
                            }).create();

                    dialog.show();
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
            return inflater.inflate(R.layout.fragment_action, null);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            View imageView = view.findViewById(R.id.icon_add);
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
//            Intent intent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, R.string.enter_speech);

//            intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "en-US");
            // Start the activity, the intent will be populated with the speech text
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode != RESULT_OK)
                return;
            if (requestCode == SPEECH_REQUEST_CODE) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                final String s = results.get(0);

                View layout = LayoutInflater.from(getContext()).inflate(R.layout.layout_confirm, null);
                TextView textView = ((TextView) layout.findViewById(R.id.textMessage));

                textView.setText(s);

                AlertDialog dialog = new AlertDialog.Builder(getContext())
                        .setTitle("Add Barcode")
                        .setView(layout)
                        .setNegativeButton(R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).setPositiveButton(R.string.confirm, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ((MainActivity) getActivity()).sendMessage(1, s);
                            }
                        }).create();

                dialog.show();
            }
        }
    }

    protected class MyPagerAdapter extends FragmentGridPagerAdapter {
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
