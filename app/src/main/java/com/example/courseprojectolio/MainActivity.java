package com.example.courseprojectolio;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText municipalitySearchText;
    private Button fetchDataButton;
    private RecyclerView recentSearchesRecyclerView;
    private MunicipalityListAdapter adapter;
    private MunicipalitySearchHistory searchHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        municipalitySearchText = findViewById(R.id.municipalitySearchText);
        fetchDataButton = findViewById(R.id.fetchDataButton);
        recentSearchesRecyclerView = findViewById(R.id.recentSearchesRecyclerView);

        searchHistory = new MunicipalitySearchHistory(this);

        List<String> initialHistory = searchHistory.getAllSearches();

        adapter = new MunicipalityListAdapter(initialHistory, this::launchTabActivity);

        recentSearchesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recentSearchesRecyclerView.setAdapter(adapter);

        fetchDataButton.setOnClickListener(v -> {
            String muni = municipalitySearchText.getText().toString().trim();
            searchHistory.addSearch(muni);
            adapter.updateData(searchHistory.getAllSearches());
            launchTabActivity(muni);
        });
    }

    private void launchTabActivity(String municipality) {
        Intent intent = new Intent(MainActivity.this, TabActivity.class);
        intent.putExtra("municipality", municipality);
        startActivity(intent);
    }
}
