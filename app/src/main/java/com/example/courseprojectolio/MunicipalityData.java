package com.example.courseprojectolio;

public class MunicipalityData {
    private final int year;
    private final String name;
    private double population;
    private double populationGrowth;
    private double employmentRate;
    private double selfSufficiency;

    public MunicipalityData(int year, String name, double population, double populationGrowth, double employmentRate, double selfSufficiency) {
        this.year = year;
        this.name = name;
        this.population = population;
        this.populationGrowth = populationGrowth;
        this.employmentRate = employmentRate;
        this.selfSufficiency = selfSufficiency;
    }

    public MunicipalityData(String name, double population, double populationGrowth, double employmentRate, double selfSufficiency) {
        this(0, name, population, populationGrowth, employmentRate, selfSufficiency);
    }

    public int getYear() {

        return year;
    }

    public String getName() {

        return name;
    }
    public double getPopulation() {
        return population;
    }
    public void setPopulation(double population) {
        this.population = population;
    }
    public double getPopulationGrowth() {
        return populationGrowth;
    }
    public void setPopulationGrowth(double populationGrowth) {
        this.populationGrowth = populationGrowth;
    }
    public double getEmploymentRate() {
        return employmentRate;
    }
    public void setEmploymentRate(double employmentRate) {
        this.employmentRate = employmentRate;
    }
    public double getSelfSufficiency() {
        return selfSufficiency;
    }
    public void setSelfSufficiency(double selfSufficiency) {
        this.selfSufficiency = selfSufficiency;
    }
}




