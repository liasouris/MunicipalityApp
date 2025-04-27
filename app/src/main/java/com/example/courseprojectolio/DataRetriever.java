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
import java.io.InputStream;
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
import java.util.concurrent.ConcurrentHashMap;

public class DataRetriever {
    // Weather APIs
    private static final String API_KEY = "3edf2211d0ca2323cc0f328f285584ad";
    private static final String CONVERT_URL = "https://api.openweathermap.org/geo/1.0/direct?q=%s&limit=5&appid=%s";
    private static final String WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s";

    // Municipality APIs
    private static final String POPULATION_URL = "https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/synt/statfin_synt_pxt_12dy.px";
    private static final String EMPLOYMENT_URL = "https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/tyokay/statfin_tyokay_pxt_115x.px";
    private static final String JOBS_URL = "https://pxdata.stat.fi:443/PxWeb/api/v1/fi/StatFin/tyokay/statfin_tyokay_pxt_125s.px";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, JsonNode> metadataCache = new ConcurrentHashMap<>();
    private final Map<String, String> municipalityCodeCache = new ConcurrentHashMap<>();

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

    public MunicipalityData getMunicipalityData(Context context, String municipality) throws IOException {
        // Population
        String popCode = getMunicipalityCode(POPULATION_URL, municipality, 1);
        if (popCode == null) return null;

        JsonNode popResult = postJson(POPULATION_URL, createPopulationQuery(context, popCode));
        JsonNode popVals = popResult.get("value");
        int lastPopIndex = popVals.size() - 1;
        double popGrowth = popVals.get(lastPopIndex - 1).asDouble();
        double population = popVals.get(lastPopIndex).asDouble();

        // Employment
        String empCode = getMunicipalityCode(EMPLOYMENT_URL, municipality, 0);
        JsonNode empResult = postJson(EMPLOYMENT_URL, createEmploymentQuery(context, empCode));
        JsonNode empVals = empResult.get("value");
        double employmentRate = empVals.get(empVals.size() - 1).asDouble();

        // Job self-sufficiency
        String jobCode = getMunicipalityCode(JOBS_URL, municipality, 1);
        JsonNode jobResult = postJson(JOBS_URL, createJobQuery(context, jobCode));
        JsonNode jobVals = jobResult.get("value");
        double selfSufficiency = jobVals.get(jobVals.size() - 1).asDouble();

        return new MunicipalityData(municipality, population, popGrowth, employmentRate, selfSufficiency);
    }

    private String getMunicipalityCode(String apiUrl, String municipality, int varIndex) throws IOException {
        String cacheKey = apiUrl + ":" + municipality;
        if (municipalityCodeCache.containsKey(cacheKey)) {
            return municipalityCodeCache.get(cacheKey);
        }

        JsonNode meta = getCachedMetadata(apiUrl);
        String code = extractCodes(meta, varIndex).get(municipality);
        if (code != null) {
            municipalityCodeCache.put(cacheKey, code);
        }
        return code;
    }

    //Added to cache metadata and make app run faster
    private JsonNode getCachedMetadata(String url) throws IOException {
        if (!metadataCache.containsKey(url)) {
            metadataCache.put(url, objectMapper.readTree(new URL(url)));
        }
        return metadataCache.get(url);
    }

    private ObjectNode createPopulationQuery(Context context) throws IOException {
        try (InputStream is = context.getResources().openRawResource(R.raw.query_population)) {
            return (ObjectNode) objectMapper.readTree(is);
        }
    }

    private ObjectNode createPopulationQuery(Context context, String code) throws IOException {
        ObjectNode root = createPopulationQuery(context);
        ArrayNode values = ((ObjectNode)((ArrayNode)root.get("query")).get(0).get("selection")).putArray("values");
        values.add(code);
        return root;
    }

    private ObjectNode createEmploymentQuery(Context context) throws IOException {
        try (InputStream is = context.getResources().openRawResource(R.raw.query_employment)) {
            return (ObjectNode) objectMapper.readTree(is);
        }
    }

    private ObjectNode createEmploymentQuery(Context context, String code) throws IOException {
        ObjectNode root = createEmploymentQuery(context);
        ArrayNode values = ((ObjectNode)((ArrayNode)root.get("query")).get(0).get("selection")).putArray("values");
        values.add(code);
        return root;
    }

    private ObjectNode createJobQuery(Context context) throws IOException {
        try (InputStream is = context.getResources().openRawResource(R.raw.query_selfsufficiency)) {
            return (ObjectNode) objectMapper.readTree(is);
        }
    }

    private ObjectNode createJobQuery(Context context, String code) throws IOException {
        ObjectNode root = createJobQuery(context);
        ArrayNode values = ((ObjectNode)((ArrayNode)root.get("query")).get(0).get("selection")).putArray("values");
        values.add(code);
        return root;
    }

    public List<PopulationData> getPopulationHistory(Context context, String municipality) throws IOException {
        String popCode = getMunicipalityCode(POPULATION_URL, municipality, 1);
        if (popCode == null) {
            Log.e("DataRetriever", "Municipality code not found for: " + municipality);
            return null;
        }

        JsonNode popResult = postJson(POPULATION_URL, createPopulationQuery(context, popCode));
        if (popResult == null) {
            Log.e("DataRetriever", "Null response from API");
            return null;
        }

        ObjectNode vuosiNode = (ObjectNode) popResult.path("dimension").path("Vuosi");
        ObjectNode tiedotNode = (ObjectNode) popResult.path("dimension").path("Tiedot");

        List<String> years = sortDimensionKeys(vuosiNode);
        List<String> metrics = sortDimensionKeys(tiedotNode);
        JsonNode valuesNode = popResult.path("value");

        List<PopulationData> history = new ArrayList<>(years.size());
        for (int i = 0; i < years.size(); i++) {
            String year = years.get(i);
            double population = valuesNode.get(i * metrics.size()).asDouble();
            double populationChange = valuesNode.get(i * metrics.size() + 1).asDouble();
            history.add(new PopulationData(year, population, populationChange));
        }

        return history;
    }

    public WeatherData getWeatherData(String municipality) {
        try {
            String q = Uri.encode(municipality.trim());
            String geoEndpoint = String.format(CONVERT_URL, q, API_KEY);
            JsonNode areas = objectMapper.readTree(new URL(geoEndpoint));

            if (!areas.isArray() || areas.isEmpty()) {
                throw new RuntimeException("No geocoding result for " + municipality);
            }
            JsonNode loc = areas.get(0);

            String latitude  = loc.get("lat").asText();
            String longitude = loc.get("lon").asText();

            String weatherEndpoint = String.format(WEATHER_URL, latitude, longitude, API_KEY);
            JsonNode weatherJson = objectMapper.readTree(new URL(weatherEndpoint));
            JsonNode w0 = weatherJson.get("weather").get(0);

            return new WeatherData(
                    weatherJson.get("name").asText(),
                    w0.get("main").asText(),
                    w0.get("description").asText(),
                    weatherJson.get("main").get("temp").asText(),
                    weatherJson.get("wind").get("speed").asText(),
                    w0.get("icon").asText()
            );

        } catch (IOException|NullPointerException e) {
            throw new RuntimeException("Failed to fetch weather for " + municipality, e);
        }
    }

    private JsonNode postJson(String urlString, JsonNode jsonBody) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(jsonBody));
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                return objectMapper.readTree(br);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private List<String> sortDimensionKeys(ObjectNode dimensionNode) {
        ObjectNode indexNode = (ObjectNode) dimensionNode.path("category").path("index");
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

    private HashMap<String, String> extractCodes(JsonNode root, int varIndex) {
        JsonNode var = root.get("variables").get(varIndex);
        List<String> codes = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        var.get("values").forEach(v -> codes.add(v.asText()));
        var.get("valueTexts").forEach(t -> labels.add(t.asText()));

        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < labels.size(); i++) {
            map.put(labels.get(i), codes.get(i));
        }
        return map;
    }
}