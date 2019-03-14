package com.esri.arcgis.demo.model;

import java.util.Calendar;
import java.util.Date;

public class Patient {

  private String patientId;
  private String firstName;
  private String lastName;
  private String ssn = "000-00-0000";
  private Date dob;
  private String sex = "m";
  private String race;
  private String religion = "N/A";
  private String occupation;
  private int houseNumber;
  private String street;
  private String city;
  private String state = "CA";
  private int zip = 92373;
  private String contactNumber = "000-000-0000";

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getSsn() {
    return ssn;
  }

  public void setSsn(String ssn) {
    this.ssn = ssn;
  }

  public Date getDob() {
    return dob;
  }

  public void setDob(Date dob) {
    this.dob = dob;
  }

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

  public String getRace() {
    return race;
  }

  public void setRace(String race) {
    this.race = race;
  }

  public String getReligion() {
    return religion;
  }

  public void setReligion(String religion) {
    this.religion = religion;
  }

  public String getOccupation() {
    return occupation;
  }

  public void setOccupation(String occupation) {
    this.occupation = occupation;
  }

  public int getHouseNumber() {
    return houseNumber;
  }

  public void setHouseNumber(int houseNumber) {
    this.houseNumber = houseNumber;
  }

  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public int getZip() {
    return zip;
  }

  public void setZip(int zip) {
    this.zip = zip;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public void setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
  }

  public String toSQLInsertStatement() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(dob);
    String insert = "INSERT INTO patients VALUES('" + patientId + "', '" + firstName + "', '" + lastName + "', '" + ssn + "', '" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "', '" + sex + "', '" +
        race + "', '" + religion + "', '" + occupation + "', " + houseNumber + ", '" + street + "', '" + city + "', '" + state + "', " + zip + ", '" + contactNumber + "');";
    return insert;
  }
}
