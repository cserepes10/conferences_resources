package com.esri.arcgis.demo;

import com.esri.arcgis.demo.model.Appointment;
import com.esri.arcgis.demo.model.AuditLogRecord;
import com.esri.arcgis.demo.model.Patient;
import com.esri.arcgis.demo.model.Provider;
import com.esri.arcgis.demo.util.DBUtil;
import com.esri.arcgis.demo.util.PreparedStatementData;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverManagerConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;


import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * This class provides access to EMR database for accessing patient information.
 */
public class EMR {

  // Connection file properties name
  private static final String CONN_FILE_NAME = "jdbc-connection.properties";

  // SQL Statements
  private static final String GET_PATIENT_APPOINTMENTS = "SELECT * FROM appointments WHERE patient_id=? order by apt_date_time desc";
  private static final String INSERT_AUDIT_LOG = "INSERT INTO audit_logs VALUES(?,?,?,?,?)";
  private static final String GET_PATIENT = "SELECT * FROM patients where patient_id=?";
  private static final String GET_PROVIDER = "SELECT * FROM providers where provider_id=?";

  // The connection to the EMR database
  private DataSource dataSource;

  // Cache to store queried information
  private Map<String, Patient> patientCache = new HashMap<>();
  private Map<String, Provider> providerCache = new HashMap<>();
  private Map<String, List<Appointment>> patientAppointments = new HashMap<>();

  public EMR() {
    connect();
  }


  /**
   * Returns list of patient appointments.
   * @param patientId
   * @return
   */
  public List<Appointment> getPatientAppointments(String patientId) {
    if(patientAppointments.containsKey(patientId)) {
      return patientAppointments.get(patientId);
    }

    List<Appointment> retVal = new ArrayList<>();

    PreparedStatementData psd = new PreparedStatementData(1);
    psd.setQueryStr(GET_PATIENT_APPOINTMENTS);
    psd.addParameter(0, PreparedStatementData.ParameterType.STRING, patientId);

    try {
      String[][] rs = DBUtil.doQuery(psd, dataSource);
      if (rs == null || rs.length == 0) {
        return retVal;
      } else {
        for (String[] r : rs) {
          Appointment apt = new Appointment();
          apt.setPatientId(r[0]);
          apt.setAccessTime(Timestamp.valueOf(r[1]));
          apt.setLocation(r[2]);
          apt.setProvider(r[3]);
          retVal.add(apt);
        }

        patientAppointments.put(patientId, retVal);
        return retVal;
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Patient getPatient(String patientId) {
    if(patientCache.containsKey(patientId)) {
      return patientCache.get(patientId);
    }
    PreparedStatementData psd = new PreparedStatementData(1);
    psd.setQueryStr(GET_PATIENT);
    psd.addParameter(0, PreparedStatementData.ParameterType.STRING, patientId);

    try {
      String[][] rs = DBUtil.doQuery(psd, dataSource);
      if (rs == null || rs.length == 0) {
        return null;
      } else {
        for (String[] r : rs) {
          Patient pt = new Patient();
          pt.setPatientId(r[0]);
          pt.setFirstName(r[1]);
          pt.setLastName(r[2]);
          pt.setSsn(r[3]);
          pt.setDob(Date.valueOf(r[4]));
          pt.setSex(r[5]);
          pt.setRace(r[6]);
          pt.setReligion(r[7]);
          pt.setOccupation(r[8]);
          try {
            pt.setHouseNumber(Integer.parseInt(r[9]));
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          pt.setStreet(r[10]);
          pt.setCity(r[11]);
          pt.setState(r[12]);
          try {
            pt.setZip(Integer.parseInt(r[13]));
          } catch (NumberFormatException e) {
            e.printStackTrace();
          }
          pt.setContactNumber(r[14]);
          patientCache.put(patientId, pt);
          return pt;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  public Provider getProvider(String providerId) {
    if(providerCache.containsKey(providerId)) {
      return providerCache.get(providerId);
    }
    PreparedStatementData psd = new PreparedStatementData(1);
    psd.setQueryStr(GET_PROVIDER);
    psd.addParameter(0, PreparedStatementData.ParameterType.STRING, providerId);

    try {
      String[][] rs = DBUtil.doQuery(psd, dataSource);
      if (rs == null || rs.length == 0) {
        return null;
      } else {
        for (String[] r : rs) {
          Provider provider = new Provider();
          provider.setProviderId(r[0]);
          provider.setFirstName(r[1]);
          provider.setLastName(r[2]);
          provider.setSsn(r[3]);
          provider.setDob(Date.valueOf(r[4]));
          provider.setSex(r[5]);
          provider.setProviderType(r[6]);
          provider.setCredentials(r[7]);
          provider.setLicense(r[8]);
          provider.setLastLogin(Timestamp.valueOf(r[9]));
          provider.setLastLoginLocation(r[10]);
          providerCache.put(providerId, provider);
          return provider;
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return null;

  }

  /**
   * Stores audit log records in the EMR database.
   * @param auditLogRecord
   */
  public int storeAuditLog(AuditLogRecord auditLogRecord) {
    //TODO
    PreparedStatementData psd = new PreparedStatementData(5);
    psd.setQueryStr(INSERT_AUDIT_LOG);
    psd.addParameter(0, PreparedStatementData.ParameterType.STRING, auditLogRecord.getPatientId());
    psd.addParameter(1, PreparedStatementData.ParameterType.STRING, auditLogRecord.getProvider());
    psd.addParameter(2, PreparedStatementData.ParameterType.STRING, auditLogRecord.getProviderType());
    psd.addParameter(3, PreparedStatementData.ParameterType.TIMESTAMP, Timestamp.from(auditLogRecord.getAccessTime().toInstant()));
    psd.addParameter(4, PreparedStatementData.ParameterType.STRING, auditLogRecord.getLocation());
    try {
      return DBUtil.doUpdate(psd, dataSource);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }


  private void connect() {
    //
    // First we load the underlying JDBC driver.
    // You need this if you don't use the jdbc.drivers
    // system property.
    //
    System.out.println("Loading underlying JDBC driver.");
    try {
      Class.forName("org.postgresql.Driver");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }

    dataSource = setupDataSource();
  }

  private DataSource setupDataSource() {
    InputStream is = getClass().getClassLoader().getResourceAsStream(CONN_FILE_NAME);
    if(is == null) {
      throw new RuntimeException("jdbc-connection.properties file not found. This file is needed to connect to the database.");
    }
    String hostname = "localhost";
    String port = "5432";
    String database = "emrdb";
    String username = "admin";
    String password = "admin";
    try {
      Properties props = new Properties();
      props.load(is);
      hostname = props.getProperty("HOSTNAME");
      port = props.getProperty("PORT");
      database = props.getProperty("DATABASE");
      username = props.getProperty("USERNAME");
      password = props.getProperty("PASSWORD");
    } catch(Exception e) {
      throw new RuntimeException(e);
    } finally {
      if(is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    String connectURI = "jdbc:postgresql://" + hostname + ":" + port + "/" + database;

    //
    // First, we'll create a ConnectionFactory that the
    // pool will use to create Connections.
    // We'll use the DriverManagerConnectionFactory,
    // using the connect string passed in the command line
    // arguments.
    //
    Properties props = new Properties();
    props.setProperty("user", username);
    props.setProperty("password", password);
    ConnectionFactory connectionFactory = new DriverManagerConnectionFactory(connectURI, props);

    //
    // Next we'll create the PoolableConnectionFactory, which wraps
    // the "real" Connections created by the ConnectionFactory with
    // the classes that implement the pooling functionality.
    //
    PoolableConnectionFactory poolableConnectionFactory =
        new PoolableConnectionFactory(connectionFactory, null);

    //
    // Now we'll need a ObjectPool that serves as the
    // actual pool of connections.
    //
    // We'll use a GenericObjectPool instance, although
    // any ObjectPool implementation will suffice.
    //
    ObjectPool<PoolableConnection> connectionPool =
        new GenericObjectPool<>(poolableConnectionFactory);

    // Set the factory's pool property to the owning pool
    poolableConnectionFactory.setPool(connectionPool);

    //
    // Finally, we create the PoolingDriver itself,
    // passing in the object pool we created.
    //
    PoolingDataSource<PoolableConnection> dataSource =
        new PoolingDataSource<>(connectionPool);

    return dataSource;
  }












}
