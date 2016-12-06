package com.m3l3m01t.myth;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


/**
 * Created by jiff.shen on 16/11/12.
 */
public abstract class MythFragment extends Fragment {
    protected static final String ARG_SECTION_NUMBER = "section_number";
    protected static final String ARG_SECTION_NAME = "section_name";
    private final static Class<?>[] fragments = new Class<?>[]{
            ContactsFragment.class,
            CalendarFragment.class,
            EmailFragment.class,

            SMSFragment.class
    };
    private final static String[] sectionNames = new String[]{
            "Contact", "Calendar", "Email", "SMS"
    };
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */

    protected int mSectionIndex;

    protected MythFragment() {
    }

    public static int getFragments() {
        return fragments.length;
    }


    public static String getSectionName(int index) {
        return sectionNames[index];
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MythFragment newInstance(int sectionNumber) {
        Class<? extends MythFragment> klz = (Class<? extends MythFragment>) fragments[sectionNumber];
        MythFragment fragment = null;
        try {
            Constructor<? extends MythFragment> constructor = klz.getConstructor();
            fragment = constructor.newInstance();

            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);

            fragment.setArguments(args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return fragment;
    }


}

