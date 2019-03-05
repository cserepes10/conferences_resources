package com.esri.arcgis.demo.model;

import java.util.Date;

/**
 * This class represents an audit record in the EMR database.
 */
public class AuditLogRecord {

  private String patientId;

  private String provider;

  private String providerType;

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

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
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
}
