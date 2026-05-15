package com.freshworks.freshindex.test.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
class Coordinates{
    public double latitude;
    public double longitude;
}

@Getter
@Setter
class CreditCard{
    public String number;
    public String cvv;
    public String issuer;
}

@Getter
@Setter
class Job{
    public String title;
    public String descriptor;
    public String area;
    public String type;
    public String company;
}

@Getter
@Setter
class Location{
    public String street;
    public String city;
    public String state;
    public String country;
    public String zip;
    public Coordinates coordinates;
}

@Getter
@Setter
class Name{
    public String first;
    public String middle;
    public String last;
}

@Getter
@Setter
public class Asset{
    public String status;
    public Name name;
    public String username;
    public String password;
    public ArrayList<String> emails;
    public String phoneNumber;
    public Location location;
    public String website;
    public String domain;
    public Job job;
    public CreditCard creditCard;
    public String uuid;
    public String objectId;
}


