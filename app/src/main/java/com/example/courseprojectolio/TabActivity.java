package com.example.courseprojectolio;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TabActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        String municipality = getIntent().getStringExtra("municipality");
        if (municipality == null) {
            municipality = "Helsinki";
        }

        viewPager = findViewById(R.id.view_pager);
        tabLayout = findViewById(R.id.tab_layout);

        TabPagerAdapter adapter = new TabPagerAdapter(this, municipality);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Municipality");
                            break;
                        case 1:
                            tab.setText("Weather");
                            break;
                        case 2:
                            tab.setText("Home");
                            break;
                    }
                }
        ).attach();
    }
}

