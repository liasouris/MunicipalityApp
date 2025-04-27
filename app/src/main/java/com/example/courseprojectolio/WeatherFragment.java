package com.example.courseprojectolio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;

public class WeatherFragment extends Fragment {
    private ImageView imageWeatherIcon;
    private TextView municipalityNameTv;
    private TextView weatherMainTv;
    private TextView weatherDescriptionTv;
    private TextView temperatureTv;
    private TextView windSpeedTv;

    private final DataRetriever dataRetriever = new DataRetriever();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_weather, container, false);

        imageWeatherIcon = view.findViewById(R.id.imageWeatherIcon);
        municipalityNameTv = view.findViewById(R.id.textMunicipalityName);
        weatherMainTv = view.findViewById(R.id.textWeatherMain);
        weatherDescriptionTv = view.findViewById(R.id.textWeatherDescription);
        temperatureTv = view.findViewById(R.id.textTemperature);
        windSpeedTv = view.findViewById(R.id.textWindSpeed);

        Bundle args = getArguments();

        final String muni = (args != null && args.containsKey("municipality")) ? args.getString("municipality") : "Helsinki";

        municipalityNameTv.setText(muni);

        new Thread(() -> {
            try {
                WeatherData wd = dataRetriever.getWeatherData(muni);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUI(wd));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        return view;
    }


    private void updateUI(WeatherData wd) {
        municipalityNameTv.setText(wd.getCityName());
        weatherMainTv.setText(wd.getMain());
        weatherDescriptionTv.setText(wd.getDescription());
        String celsius = wd.convertToCelsiusFromKelvin(wd.getTemperature());
        temperatureTv.setText(celsius + "°C");
        windSpeedTv.setText("Wind " + wd.getWindSpeed() + " m/s");

        String iconUrl = "https://openweathermap.org/img/wn/" + wd.getIcon() + "@2x.png";
        Glide.with(this).load(iconUrl).into(imageWeatherIcon);
    }
}
