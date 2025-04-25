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

    @NonNull @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                WeatherFragment wf = new WeatherFragment();
                Bundle argsW = new Bundle();
                argsW.putString("municipality", municipality);
                wf.setArguments(argsW);
                return wf;
            case 2:
                return new ReturnHomeFragment();

            default:
                MunicipalityFragment mf = new MunicipalityFragment();
                Bundle argsM = new Bundle();
                argsM.putString("municipality", municipality);
                mf.setArguments(argsM);
                return mf;
        }
    }

    @Override public int getItemCount() {
        return 3;
    }
}