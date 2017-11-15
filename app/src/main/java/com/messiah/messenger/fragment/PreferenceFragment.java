package com.messiah.messenger.fragment;

import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import com.messiah.messenger.R;

public class PreferenceFragment extends PreferenceFragmentCompat {

    public PreferenceFragment() {

    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);
    }



}
