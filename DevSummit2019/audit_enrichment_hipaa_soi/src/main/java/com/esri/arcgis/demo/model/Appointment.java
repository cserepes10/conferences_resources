package com.esri.arcgis.demo.model;

import java.util.Date;

public class Appointment {

  private String patientId;
  private String provider;
  private Date accessTime;
  private String location;

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public Date getAccessTime() {
    return accessTime;
  }

  public void setAccessTime(Date accessTime) {
    this.accessTime = accessTime;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String toSQLInsertStatement() {
    return "INSERT INTO appointments VALUES ('" + patientId + "', to_timestamp(" + accessTime.getTime() + "/1000), '" + location + "', '" + provider + "');";
  }
}
