package com.example.courseprojectolio;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

public class CompareFragment extends Fragment {

    private EditText muni1Edit;
    private EditText muni2Edit;
    private Button   compareButton;
    private TextView muni1DataTv;
    private TextView muni2DataTv;

    private DataRetriever dataRetriever;

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_compare, container, false);

        muni1Edit = view.findViewById(R.id.Municipality1EditText);
        muni2Edit = view.findViewById(R.id.Municipality2EditText);
        compareButton = view.findViewById(R.id.CompareMunicipalityButton);
        muni1DataTv = view.findViewById(R.id.Municipality1DataTextView);
        muni2DataTv = view.findViewById(R.id.Municipality2DataTextView);

        dataRetriever = new DataRetriever();

        compareButton.setOnClickListener(v -> {
            String m1 = muni1Edit.getText().toString().trim();
            String m2 = muni2Edit.getText().toString().trim();

            muni1DataTv.setText("Loading…");
            muni2DataTv.setText("Loading…");

            new Thread(() -> {
                try {
                    MunicipalityData d1 = dataRetriever.getMunicipalityData(requireContext(), m1);
                    MunicipalityData d2 = dataRetriever.getMunicipalityData(requireContext(), m2);

                    requireActivity().runOnUiThread(() -> {
                        if (d1 != null) {
                            muni1DataTv.setText(String.format("Population: %,d\nEmployment: %.1f%%", (long) d1.getPopulation(), d1.getEmploymentRate())
                            );
                        } else {
                            muni1DataTv.setText("No data for “" + m1 + "”");
                        }

                        if (d2 != null) {
                            muni2DataTv.setText(String.format("Population: %,d\nEmployment: %.1f%%", (long) d2.getPopulation(), d2.getEmploymentRate())
                            );
                        } else {
                            muni2DataTv.setText("No data for “" + m2 + "”");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    requireActivity().runOnUiThread(() -> {
                        muni1DataTv.setText("Error fetching data");
                        muni2DataTv.setText("Error fetching data");
                    });
                }
            }).start();
        });

        return view;
    }
}
