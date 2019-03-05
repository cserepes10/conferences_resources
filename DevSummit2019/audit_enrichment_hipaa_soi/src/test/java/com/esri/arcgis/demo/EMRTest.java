package com.esri.arcgis.demo;

import com.esri.arcgis.demo.model.Appointment;
import com.esri.arcgis.demo.model.AuditLogRecord;
import com.esri.arcgis.demo.model.Patient;
import com.esri.arcgis.demo.model.Provider;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class EMRTest {

  public static EMR emr = new EMR();

  @Test
  public void getPatientAppointments() {

    List<Appointment> appointmentList = emr.getPatientAppointments("3");
    Assert.assertTrue(appointmentList.size() > 0 );
    System.out.println(appointmentList.get(0).getPatientId());
    System.out.println(appointmentList.get(0).getProvider());
    System.out.println(appointmentList.get(0).getAccessTime());
  }

  @Test
  public void storeAuditLog() {
    AuditLogRecord alr = new AuditLogRecord();
    alr.setPatientId("3");
    alr.setProvider("provider00053");
    alr.setProviderType(emr.getProvider("provider00053").getProviderType());
    alr.setAccessTime(new Date());
    alr.setLocation("102");
    Assert.assertEquals(emr.storeAuditLog(alr), 1);
  }

  @Test
  public void getPatient() {
    Patient patient = emr.getPatient("3");
    Assert.assertNotNull(patient);
    System.out.println(patient.getFirstName());
    System.out.println(patient.getLastName());
    System.out.println(patient.getDob());
  }

  @Test
  public void getProvider() {
    Provider provider = emr.getProvider("provider00053");
    Assert.assertNotNull(provider);
    System.out.println(provider.getFirstName());
    System.out.println(provider.getLastName());
    System.out.println(provider.getDob());
    System.out.println(provider.getLastLogin());
  }
}