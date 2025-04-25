package com.example.courseprojectolio;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class DataRetriever {
    //Weather APIs
    private static final String API_KEY = "1703bdfd503c84d8e12227cd540f8262";
    private static final String GEOCODING_URL = "https://api.openweathermap.org/geo/1.0/direct?q=%s,FI&limit=5&appid=%s";
    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s";

    // Municipality APIs
    private static final String POPULATION_URL = "https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/synt/statfin_synt_pxt_12dy.px";
    private static final String EMPLOYMENT_URL = "https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/tyokay/statfin_tyokay_pxt_115x.px";
    private static final String JOBS_URL = "https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/tyokay/statfin_tyokay_pxt_125s.px";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class PopulationData {
        public final String year;
        public final double population;
        public final double populationChange;

        public PopulationData(String year, double population, double populationChange) {
            this.year = year;
            this.population = population;
            this.populationChange = populationChange;
        }
    }

    public MunicipalityData getMunicipalityData(Context context, String municipality) {
        try {
            // Population
            JsonNode popMeta = objectMapper.readTree(new URL(POPULATION_URL));
            String popCode = extractCodes(popMeta, 1).get(municipality);
            if (popCode == null) return null;

            ObjectNode popRoot = (ObjectNode) objectMapper.readTree(context.getResources().openRawResource(R.raw.query_population));

            ArrayNode popQueryArr = (ArrayNode) popRoot.get("query");
            ObjectNode popQueryObj = (ObjectNode) popQueryArr.get(0);
            ObjectNode popSelection = (ObjectNode) popQueryObj.get("selection");
            ArrayNode popValues = popSelection.putArray("values");
            popValues.add(popCode);

            JsonNode popResult = postJson(POPULATION_URL, popRoot);
            JsonNode popVals = popResult.get("value");
            int lastPopIndex = popVals.size() - 1;
            double popGrowth = popVals.get(lastPopIndex - 1).asDouble();
            double population = popVals.get(lastPopIndex).asDouble();

            // Employment
            JsonNode empMeta = objectMapper.readTree(new URL(EMPLOYMENT_URL));
            String empCode = extractCodes(empMeta, 0).get(municipality);

            ObjectNode empRoot = (ObjectNode) objectMapper.readTree(context.getResources().openRawResource(R.raw.query_employment));

            ArrayNode empQueryArr = (ArrayNode) empRoot.get("query");
            ObjectNode empQueryObj = (ObjectNode) empQueryArr.get(0);
            ObjectNode empSelection = (ObjectNode) empQueryObj.get("selection");
            ArrayNode empValues = empSelection.putArray("values");
            empValues.add(empCode);

            JsonNode empResult = postJson(EMPLOYMENT_URL, empRoot);
            JsonNode empVals = empResult.get("value");
            double employmentRate = empVals.get(empVals.size() - 1).asDouble();

            //Job self-sufficiency
            JsonNode jobMeta = objectMapper.readTree(new URL(JOBS_URL));
            String jobCode = extractCodes(jobMeta, 1).get(municipality);

            ObjectNode jobRoot = (ObjectNode) objectMapper.readTree(context.getResources().openRawResource(R.raw.query_selfsufficiency));

            ArrayNode jobQueryArr = (ArrayNode) jobRoot.get("query");
            ObjectNode jobQueryObj = (ObjectNode) jobQueryArr.get(0);
            ObjectNode jobSelection = (ObjectNode) jobQueryObj.get("selection");
            ArrayNode jobValues = jobSelection.putArray("values");
            jobValues.add(jobCode);

            JsonNode jobResult = postJson(JOBS_URL, jobRoot);
            JsonNode jobVals = jobResult.get("value");
            double selfSufficiency = jobVals.get(jobVals.size() - 1).asDouble();

            return new MunicipalityData(municipality, population, popGrowth, employmentRate, selfSufficiency);

        } catch (IOException ioe) {
            throw new RuntimeException("Failed to retrieve municipality data", ioe);
        }
    }

    //For AnyChart populating
    public List<PopulationData> getPopulationHistory(Context context, String municipality) {
        try {
            // 1) Fetch metadata and extract the municipality code
            JsonNode popMeta = objectMapper.readTree(new URL(POPULATION_URL));
            String popCode = extractCodes(popMeta, 1).get(municipality);
            if (popCode == null) {
                Log.e("DataRetriever", "Municipality code not found for: " + municipality);
                return null;
            }

            ObjectNode popRoot = (ObjectNode) objectMapper.readTree(
                    context.getResources().openRawResource(R.raw.query_population)
            );
            ((ObjectNode)((ArrayNode)popRoot.get("query")).get(0)
                    .get("selection")).putArray("values").add(popCode);

            JsonNode popResult = postJson(POPULATION_URL, popRoot);
            if (popResult == null) {
                Log.e("DataRetriever", "Null response from API");
                return null;
            }

            ObjectNode vuosiNode = (ObjectNode) popResult
                    .path("dimension")
                    .path("Vuosi");
            ObjectNode tiedotNode = (ObjectNode) popResult
                    .path("dimension")
                    .path("Tiedot");

            List<String> years   = sortDimensionKeys(vuosiNode);
            List<String> metrics = sortDimensionKeys(tiedotNode);

            int yearCount   = years.size();
            int metricCount = metrics.size();
            JsonNode valuesNode = popResult.path("value");
            if (!valuesNode.isArray() || valuesNode.size() != yearCount * metricCount) {
                Log.e("DataRetriever", "Data count mismatch: years=" + yearCount
                        + " metrics=" + metricCount + " values=" + valuesNode.size());
                return null;
            }

            List<PopulationData> history = new ArrayList<>();
            for (int i = 0; i < yearCount; i++) {
                String year = years.get(i);
                double population       = valuesNode.get(i * metricCount).asDouble();
                double populationChange = valuesNode.get(i * metricCount + 1).asDouble();
                history.add(new PopulationData(year, population, populationChange));
            }

            return history;

        } catch (IOException ioe) {
            throw new RuntimeException("Failed to retrieve municipality data", ioe);
        }
    }

    private List<String> sortDimensionKeys(ObjectNode dimensionNode) {
        ObjectNode indexNode = (ObjectNode)
                dimensionNode.path("category").path("index");

        Map<String, Integer> indexMap = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = indexNode.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            indexMap.put(e.getKey(), e.getValue().asInt());
        }

        List<String> sortedKeys = new ArrayList<>(indexMap.keySet());
        sortedKeys.sort(Comparator.comparingInt(indexMap::get));
        return sortedKeys;
    }

    //Fetches weather data for a given municipality
    public WeatherData getWeatherData(String municipality) {
        if (municipality == null || municipality.trim().isEmpty()) {
            throw new IllegalArgumentException("Municipality name cannot be empty");
        }
        try {
            String encoded = Uri.encode(municipality.trim());
            String geoUrl = String.format(GEOCODING_URL, encoded, API_KEY);
            JsonNode geoArr = objectMapper.readTree(new URL(geoUrl));
            if (!geoArr.isArray() || geoArr.isEmpty()) {
                throw new RuntimeException("No geocoding result for " + municipality);
            }
            JsonNode loc = geoArr.get(0);
            double lat = loc.get("lat").asDouble();
            double lon = loc.get("lon").asDouble();

            String weatherUrl = String.format(WEATHER_URL, lat, lon, API_KEY);
            JsonNode wj = objectMapper.readTree(new URL(weatherUrl));
            JsonNode w0 = wj.get("weather").get(0);

            return new WeatherData(
                    wj.get("name").asText(),
                    w0.get("main").asText(),
                    w0.get("description").asText(),
                    wj.get("main").get("temp").asText(),
                    wj.get("wind").get("speed").asText(),
                    w0.get("icon").asText()
            );
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to fetch weather data for " + municipality, ioe);
        }
    }

    private JsonNode postJson(String urlString, JsonNode jsonBody) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(jsonBody));
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line.trim());
            }
            return objectMapper.readTree(sb.toString());
        }
    }

    private HashMap<String, String> extractCodes(JsonNode root, int varIndex) {
        JsonNode var = root.get("variables").get(varIndex);
        ArrayList<String> codes = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        for (JsonNode v : var.get("values"))     codes.add(v.asText());
        for (JsonNode t : var.get("valueTexts")) labels.add(t.asText());

        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            map.put(labels.get(i), codes.get(i));
        }
        return map;
    }
}

