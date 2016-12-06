package com.m3l3m01t.myth;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

/**
 * Created by jiff.shen on 16/11/12.
 */
public class ContactsFragment extends MythFragment {
    private static final String KEY_CONSTRAINS = "constrains";
    private static final String KEY_CONSTRAIN_ARGS = "constrain_args";
    HashMap<Integer, ContactInfo> mPhotoHash = new HashMap<>();
    private View mRootView;
    private GridView mGridView;
    private LoaderManager.LoaderCallbacks<? extends Cursor> loaderCb = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == 0) {
                String selection = null;
                selection = args.getString(KEY_CONSTRAINS);

                String[] selectionArgs = args.getStringArray(KEY_CONSTRAIN_ARGS);

                return new CursorLoader(getContext(), ContactsContract.Contacts.CONTENT_URI,
                        null, selection, selectionArgs, ContactsContract.Contacts.DISPLAY_NAME);
            }

            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            if (loader.getId() == 0) {

                mGridView.setNumColumns(2);
                mGridView.setHorizontalSpacing(2);
                mGridView.setVerticalSpacing(2);
                ((CursorAdapter) mGridView.getAdapter()).changeCursor(data);

//                ((CursorAdapter)mGridView.getAdapter()).getFilter().filter(ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1");
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    };

    public List<String> getPhoneNumber(Context context, int id) {
        Vector<String> numbers = new Vector<String>();
        Cursor phones = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{
                        String.valueOf(id)
                }, null);

        if (phones.getCount() > 0) {
            while (phones.moveToNext()) {
                numbers.add(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
            }
        }

        phones.close();

        return numbers;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);

        mRootView = rootView;

        mGridView = (GridView) mRootView.findViewById(R.id.gvContacts);

        mGridView.setAdapter(new ContactsAdapter(getContext(), null, 0));


        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Bundle bundle = new Bundle();

        bundle.putString(KEY_CONSTRAINS, ContactsContract.Contacts.HAS_PHONE_NUMBER + "=?");

        bundle.putStringArray(KEY_CONSTRAIN_ARGS, new String[]{"1"});

        getLoaderManager().initLoader(0, bundle, loaderCb);
    }

    class ContactInfo {
        public RoundedBitmapDrawable photo;
        public String phoneNum;

        public ContactInfo(RoundedBitmapDrawable photo, String phoneNum) {
            this.phoneNum = phoneNum;
            this.photo = photo;
        }
    }

    class ContactsAdapter extends CursorAdapter {
        public ContactsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View layoutView = getLayoutInflater(null).inflate(R.layout.gv_contact_item, null);

//            bindView(layoutView, context, cursor);

            return layoutView;
        }

        @Override
        public void bindView(View layoutView, Context context, Cursor cursor) {
            ImageView imageView = (ImageView) layoutView.findViewById(R.id.ivContact);
            int contactId = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            layoutView.setTag(R.id.contactId, contactId);


            String primaryName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));

            ((TextView) layoutView.findViewById(R.id.tvFirstName)).setText(primaryName);

            if (mPhotoHash.containsKey(contactId)) {
                ContactInfo info = mPhotoHash.get(contactId);
                ((TextView) layoutView.findViewById(R.id.tvPhoneNum)).setText(info.phoneNum);
                imageView.setImageDrawable(info.photo);
            } else {
                String phoneNum = "";
                int hasPhoneNumber = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (hasPhoneNumber != 0) {
                    List<String> numbers = getPhoneNumber(context, contactId);
                    if (numbers.size() > 0) {
                        phoneNum = numbers.get(0);
                    }
                }

                ((TextView) layoutView.findViewById(R.id.tvPhoneNum)).setText(phoneNum);

                Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);


                InputStream photoStream = ContactsContract.Contacts.openContactPhotoInputStream(
                        context.getContentResolver(),
                        contactUri);

                RoundedBitmapDrawable rounded = null;
                if (photoStream != null) {
                    rounded = RoundedBitmapDrawableFactory.create(getResources(), photoStream);

                    rounded.setCircular(true);

                    rounded.setCornerRadius(8);

                    try {
                        photoStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                imageView.setImageDrawable(rounded);

                mPhotoHash.put(contactId, new ContactInfo(rounded, phoneNum));
            }
        }
    }

    class ContactLoader implements LoaderManager.LoaderCallbacks<Cursor> {
        protected int mContactId;

        public ContactLoader(int contactId) {
            mContactId = contactId;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (id == 1) {
                return new CursorLoader(getContext(),
                        Uri.parse(ContactsContract.CommonDataKinds.Photo.PHOTO_URI),
                        null, null, null, ContactsContract.CommonDataKinds.Photo.CONTACT_ID);
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (loader.getId() == 1) {
                mGridView.getAdapter().getCount();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {

        }
    }
}
