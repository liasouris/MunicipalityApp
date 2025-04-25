package com.example.courseprojectolio;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.anychart.AnyChart;
import com.anychart.AnyChartView;
import com.anychart.chart.common.dataentry.DataEntry;
import com.anychart.chart.common.dataentry.ValueDataEntry;
import com.anychart.charts.Cartesian;
import com.anychart.core.cartesian.series.Line;
import com.anychart.data.Mapping;
import com.anychart.data.Set;
import com.anychart.enums.MarkerType;
import com.anychart.enums.TooltipPositionMode;
import com.anychart.graphics.vector.Anchor;
import com.anychart.graphics.vector.Stroke;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MunicipalityFragment extends Fragment {
    private static final String ARG_MUNICIPALITY = "municipality";
    private TextView nameTv;
    private TextView popTv;
    private TextView popChangeTv;
    private TextView empRateTv;
    private TextView selfSuffTv;
    private AnyChartView chartView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_municipality, container, false);
    }

    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);

        nameTv = root.findViewById(R.id.MunicipalityName);
        popTv = root.findViewById(R.id.PopulationTextView);
        popChangeTv = root.findViewById(R.id.PopulationChangeTextView);
        empRateTv = root.findViewById(R.id.EmploymentRateTextView);
        selfSuffTv = root.findViewById(R.id.JobSelfSufficiencyTextView);
        chartView = root.findViewById(R.id.anyChartPopulation);

        String municipality = requireArguments().getString(ARG_MUNICIPALITY, "–");
        nameTv.setText(municipality);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            DataRetriever retriever = new DataRetriever();
            MunicipalityData data = retriever.getMunicipalityData(requireContext(), municipality);
            List<DataRetriever.PopulationData> populationHistory = retriever.getPopulationHistory(requireContext(), municipality);

            mainHandler.post(() -> {
                if (data == null) {
                    popTv.setText("Population: –");
                    popChangeTv.setText("Population Growth: –");
                    empRateTv.setText("Employment Rate: –%");
                    selfSuffTv.setText("Job Self-Sufficiency: –%");
                } else {
                    popTv.setText(String.format("Population: %.0f", data.getPopulation()));
                    popChangeTv.setText(String.format("Population Growth: %.0f", data.getPopulationGrowth()));
                    empRateTv.setText(String.format("Employment Rate: %.1f%%", data.getEmploymentRate()));
                    selfSuffTv.setText(String.format("Job Self-Sufficiency: %.1f%%", data.getSelfSufficiency()));
                }

                if (populationHistory != null && !populationHistory.isEmpty()) {
                    setupPopulationChart(populationHistory);
                }
            });
        });
    }
    private void setupPopulationChart(List<DataRetriever.PopulationData> history) {
        Cartesian cartesian = AnyChart.line();
        cartesian.animation(true);
        cartesian.title(requireArguments().getString(ARG_MUNICIPALITY) + " Population Growth");

        cartesian.yAxis(0).title("Growth");
        cartesian.xAxis(0).labels().padding(5d, 5d, 5d, 5d);

        List<DataEntry> seriesData = new ArrayList<>();
        for (DataRetriever.PopulationData data : history) {
            seriesData.add(new GrowthEntry(data.year, data.populationChange));
        }

        Set set = Set.instantiate();
        set.data(seriesData);

        Mapping growthMapping = set.mapAs("{ x: 'x', value: 'growth' }");
        Line growthSeries = cartesian.line(growthMapping);
        growthSeries.name("Growth %");
        growthSeries.stroke("4 #FF0000");

        chartView.setChart(cartesian);
    }

    private static class GrowthEntry extends ValueDataEntry {
        GrowthEntry(String year, double growth) {
            super(year, growth);
            setValue("growth", growth);
        }
    }
}