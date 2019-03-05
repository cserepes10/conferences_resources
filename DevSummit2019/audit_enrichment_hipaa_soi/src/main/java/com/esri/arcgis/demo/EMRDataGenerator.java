package com.esri.arcgis.demo;

import com.esri.arcgis.demo.model.Appointment;
import com.esri.arcgis.demo.model.Patient;
import com.esri.arcgis.demo.model.Provider;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class EMRDataGenerator {

  public static final String[] firstNames = {"Robert", "James", "John", "Mary", "Alice", "Joanna", "William", "Jack", "Wilma", "Nadine", "George", "Hailey", "Hilary", "Anna", "Andrew"};
  public static final String[] lastNames = {"Jenson", "Goyal", "Menon", "Theodore", "Major", "Watson", "Turing", "Liu", "Shah", "Kumar", "Gonzalez", "Lam", "Scott", "Matthais", "White"};
  public static final String[] races = {"Asian", "Hispanic", "White", "European", "Middle Eastern", "Black", "African", "Native", "Hawaiian"};
  public static final String[] streets = {"Main St", "Heath Ave", "San Benardino Ave", "9th St", "Foothill Blvd", "Redlands Blvd", "Cajon St", "Whitman Ave", "Church St"};
  public static final String[] cities = {"Redlands", "Rancho Cucamonga", "Rialto", "Fontana", "Loma Linda", "Colton", "San Bernardino", "Beaumont", "Riverside"};
  public static final String state = "CA";
  public static final String[] providerTypes = {"Cardiologist", "Radiologist", "Psychologist", "Internist", "Allergist", "Dentist", "Physiotherapist", "Occupational Therapist", "Nurse", "Administrator"};
  public static final String[] sex = {"m", "f"};
  public static final String[] zipcodes = {"91220", "92373", "92373", "923733", "92344", "92324", "91022", "92321", "91117"};
  public static final String[] occupation = {"Teacher", "Construction Worker", "Software Engineer", "Accountant", "Doctor", "Nurse", "HR Professional", "Pilot", "Soldier", "Driver"};
  public static final String[] locations = {"101", "102", "103A", "103B", "OPD1", "OPD2", "OT1", "OT2", "202", "203", "301A", "302B"};
  public static final String[] credentials = {"M.D.", "D.D.S.", "D.P.T.", "B.S.", "M.S.", "P.A."};

  public static final Random random = new Random();

  public static final int MAX_BOUNDS_PATIENTS = 500;
  public static final int MAX_BOUNDS_PROVIDERS = 100;


  public static  String getRandomStringFromArray(String[] array) {
    return array[random.nextInt(array.length)];
  }

  public static String generateRandomSSN() {
    return String.valueOf(random.nextInt(999)) + "-" + String.valueOf(random.nextInt(99)) + "-" + String.valueOf(random.nextInt(9999));
  }

  public static String generateRandomContactNumber() {
    return String.valueOf(random.nextInt(999)) + "-" + String.valueOf(random.nextInt(999)) + "-" + String.valueOf(random.nextInt(9999));
  }

  public static Date generateRandomDate() {
    Calendar cal = Calendar.getInstance();
    cal.set(1930 + random.nextInt(75), random.nextInt(12), random.nextInt(30));
    return cal.getTime();
  }

  public static String getRandomStringFromMap(Map<String, ?> map) {
    Set<String> keys = map.keySet();
    int randomKey = random.nextInt(map.size());
    int i=0;
    Iterator<String> iter = keys.iterator();
    while(i++  != randomKey && iter.hasNext()) {
      iter.next();
    }
    // It should never come here
    return iter.next();
  }



  public static void main(String[] args) {

    Map<String, Patient> patients = new HashMap<>();
    Map<String, Provider> providers = new HashMap<>();

    System.out.println("/* patients */");
    for(int i=0; i<MAX_BOUNDS_PATIENTS; i++) {
      Patient p = new Patient();
      p.setPatientId(String.valueOf(i));
      p.setFirstName(getRandomStringFromArray(firstNames));
      p.setLastName(getRandomStringFromArray(lastNames));
      p.setSsn(generateRandomSSN());
      p.setDob(generateRandomDate());
      p.setSex(getRandomStringFromArray(sex));
      p.setRace(getRandomStringFromArray(races));
      p.setReligion("N/A");
      p.setOccupation(getRandomStringFromArray(occupation));
      p.setHouseNumber(random.nextInt(99999));
      p.setStreet(getRandomStringFromArray(streets));
      p.setCity(getRandomStringFromArray(cities));
      p.setState(state);
      p.setZip(Integer.parseInt(getRandomStringFromArray(zipcodes)));
      p.setContactNumber(generateRandomContactNumber());
      patients.put(p.getPatientId(), p);
      System.out.println(p.toSQLInsertStatement());
    }

    System.out.println("/* providers */");
    for(int i=0; i<MAX_BOUNDS_PROVIDERS; i++) {
      Provider p = new Provider();
      p.setProviderId("provider000" + String.valueOf(i));
      p.setFirstName(getRandomStringFromArray(firstNames));
      p.setLastName(getRandomStringFromArray(lastNames));
      p.setSsn(generateRandomSSN());
      p.setDob(generateRandomDate());
      p.setSex(getRandomStringFromArray(sex));
      p.setProviderType(getRandomStringFromArray(providerTypes));
      p.setCredentials(getRandomStringFromArray(credentials));
      p.setLicense("L000" + i);
      p.setLastLogin(generateRandomDate());
      p.setLastLoginLocation(getRandomStringFromArray(locations));
      providers.put(p.getProviderId(), p);
      System.out.println(p.toSQLInsertStatement());
    }

    System.out.println("/* appointments */");
    for(int i=0; i<MAX_BOUNDS_PATIENTS; i++) {
      Appointment appointment = new Appointment();
      appointment.setPatientId(String.valueOf(i));
      appointment.setAccessTime(new Date());
      Provider provider = providers.get(getRandomStringFromMap(providers));
      appointment.setProvider(provider.getProviderId());
      appointment.setLocation(getRandomStringFromArray(locations));
      System.out.println(appointment.toSQLInsertStatement());
    }
  }
}
