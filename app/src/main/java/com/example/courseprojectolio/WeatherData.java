package com.example.courseprojectolio;

public class Weather {
    private String name;
    private String main;
    private String description;
    private String temperature;
    private String windSpeed;

    private String icon;

    public Weather(String name, String main, String description, String temperature, String windSpeed, String icon) {
        this.name = name;
        this.main = main;
        this.description = description;
        this.temperature = temperature;
        this.windSpeed = windSpeed;
        this.icon        = icon;

    }

    public String getCityName() {
        return name;
    }
    public void   setCityName(String cityName) {
        this.name = cityName;
    }

    public String getMain() {
        return main;
    }
    public void   setMain(String main) {
        this.main = main;
    }

    public String getDescription() {
        return description; }
    public void   setDescription(String description) {
        this.description = description;
    }

    public String getTemperature() {
        return temperature;
    }
    public void   setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getWindSpeed() {
        return windSpeed;
    }
    public void   setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }
    public String getIcon() {
        return icon;
    }

    public void   setIcon(String icon) {
        this.icon = icon;
    }

}


