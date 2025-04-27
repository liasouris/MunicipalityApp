package com.example.courseprojectolio;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TabPagerAdapter extends FragmentStateAdapter {
    private final String municipality;

    public TabPagerAdapter(@NonNull FragmentActivity fragmentActivity, String municipality) {
        super(fragmentActivity);
        this.municipality = municipality;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                MunicipalityFragment mf = new MunicipalityFragment();
                Bundle argsM = new Bundle();
                argsM.putString("municipality", municipality);
                mf.setArguments(argsM);
                return mf;

            case 1:
                WeatherFragment wf = new WeatherFragment();
                Bundle argsW = new Bundle();
                argsW.putString("municipality", municipality);
                wf.setArguments(argsW);
                return wf;

            case 2:
                CompareFragment cf = new CompareFragment();
                Bundle argsC = new Bundle();
                argsC.putString("municipality", municipality);
                cf.setArguments(argsC);
                return cf;

            default:
                MunicipalityFragment defaultMf = new MunicipalityFragment();
                Bundle defaultArgs = new Bundle();
                defaultArgs.putString("municipality", municipality);
                defaultMf.setArguments(defaultArgs);
                return defaultMf;
        }
    }

    @Override public int getItemCount() {
        return 3;
    }
}