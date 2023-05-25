package com.cmp354.catrental;

import com.google.firebase.Timestamp;

public class Cat {
    public Cat(String breed, String city, double lat, double longit, String name, String owner, double price, String renter, String url, Timestamp timestamp) {
        Breed = breed;
        City = city;
        Lat = lat;
        Longit = longit;
        Name = name;
        Owner = owner;
        Price = price;
        Renter = renter;
        imgURL = url;
        stamp = timestamp;
    }
    public Cat() {
        Breed = City = Name = Owner = Renter = "Empty";
        Lat = Longit = Price = 0;
    }
    public String getBreed() {
        return Breed;
    }

    public void setBreed(String breed) {
        Breed = breed;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public double getLat() {
        return Lat;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLongit() {
        return Longit;
    }

    public void setLongit(double longit) {
        Longit = longit;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getOwner() {
        return Owner;
    }

    public void setOwner(String owner) {
        Owner = owner;
    }

    public double getPrice() {
        return Price;
    }

    public void setPrice(double price) {
        Price = price;
    }

    public String getRenter() {
        return Renter;
    }

    public void setRenter(String renter) {
        Renter = renter;
    }

    public String getImgURL() {
        return imgURL;
    }

    public Timestamp getStamp() {
        return stamp;
    }

    public void setStamp(Timestamp timestamp) {
        this.stamp = timestamp;
    }

    public void setImgURL(String imgURL) {
        this.imgURL = imgURL;
    }
    // member variables
    private String Breed;
    private String City;
    private double Lat;
    private double Longit;
    private String Name;
    private String Owner;
    private double Price;
    private String Renter;

    private String imgURL;
    private Timestamp stamp;

    @Override
    public String toString() {
        return Name;
    }
}
