package com.esri.arcgis.demo.model;

import java.util.Calendar;
import java.util.Date;

public class Provider {

  private String providerId;
  private String firstName;
  private String lastName;
  private String ssn;
  private Date dob;
  private String sex;
  private String providerType;
  private String credentials;
  private String license;
  private Date lastLogin;
  private String lastLoginLocation;

  public String getProviderId() {
    return providerId;
  }

  public void setProviderId(String providerId) {
    this.providerId = providerId;
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

  public String getSex() {
    return sex;
  }

  public void setSex(String sex) {
    this.sex = sex;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getCredentials() {
    return credentials;
  }

  public void setCredentials(String credentials) {
    this.credentials = credentials;
  }

  public String getLicense() {
    return license;
  }

  public void setLicense(String license) {
    this.license = license;
  }

  public Date getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(Date lastLogin) {
    this.lastLogin = lastLogin;
  }

  public String getLastLoginLocation() {
    return lastLoginLocation;
  }

  public void setLastLoginLocation(String lastLoginLocation) {
    this.lastLoginLocation = lastLoginLocation;
  }

  public Date getDob() {
    return dob;
  }

  public void setDob(Date dob) {
    this.dob = dob;
  }

  public String toSQLInsertStatement() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(dob);
    String insert = "INSERT INTO providers VALUES('" + providerId + "', '" + firstName + "', '" + lastName + "', '" + ssn + "', '" + cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.DAY_OF_MONTH) + "', '" + sex + "', '" +
        providerType + "', '" + credentials + "', '" + license + "', to_timestamp(" + lastLogin.getTime() + "/1000),'" + lastLoginLocation + "');";
    return insert;
  }
}
