package com.esri.arcgis.demo;


import com.esri.arcgis.demo.model.Appointment;
import com.esri.arcgis.demo.model.AuditLogRecord;
import com.esri.arcgis.demo.model.Patient;
import com.esri.arcgis.demo.model.Provider;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.interop.extn.ArcGISExtension;
import com.esri.arcgis.interop.extn.ServerObjectExtProperties;
import com.esri.arcgis.server.IServerObject;
import com.esri.arcgis.server.IServerObjectExtension;
import com.esri.arcgis.server.IServerObjectHelper;
import com.esri.arcgis.server.SOIHelper;
import com.esri.arcgis.server.json.JSONArray;
import com.esri.arcgis.server.json.JSONObject;
import com.esri.arcgis.system.ILog;
import com.esri.arcgis.system.IRESTRequestHandler;
import com.esri.arcgis.system.IRequestHandler;
import com.esri.arcgis.system.IRequestHandler2;
import com.esri.arcgis.system.IWebRequestHandler;
import com.esri.arcgis.system.ServerUtilities;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 */
@ArcGISExtension
@ServerObjectExtProperties(
    displayName = "Auditing And Compliance SOI",
    description = "Writes audit logs and integrates information from EMR database.",
    interceptor = true,
    servicetype = "MapService")
public class AuditAndComplianceSOI implements IServerObjectExtension, IRESTRequestHandler, IWebRequestHandler,
    IRequestHandler2, IRequestHandler {

  private static final long serialVersionUID = 1L;
  private static final String ARCGISHOME_ENV = "AGSSERVER";
  private ILog serverLog;
  private IServerObject so;
  private SOIHelper soiHelper;
  private EMR emrDB;

  /**
   * init() is called once, when the instance of the SOE/SOI is created.
   *
   * @param soh the IServerObjectHelper
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  public void init(IServerObjectHelper soh) throws IOException, AutomationException {
    try {
      // Get reference to server logger utility
      this.serverLog = ServerUtilities.getServerLogger();
      // Log message with server
      this.serverLog.addMessage(3, 200, "Initialized " + this.getClass().getName() + " SOI.");
      this.so = soh.getServerObject();
      String arcgisHome = getArcGISHomeDir();
      // Requires path to ArcGIS installation directory
      if(arcgisHome == null) {
        serverLog.addMessage(1, 200,"Could not get ArcGIS home directory. Check if environment variable " + ARCGISHOME_ENV + " is set.");
        throw new IOException("Could not get ArcGIS home directory. Check if environment variable " + ARCGISHOME_ENV + " is set.");
      }

      if(arcgisHome != null && !arcgisHome.endsWith(File.separator)) arcgisHome += File.separator;
      // Load the SOI helper.
      this.soiHelper = new SOIHelper(arcgisHome + "XmlSchema" + File.separator + "MapServer.wsdl");

      // Connect to the EMR database
      // To the EMR database we can write audit logs and fetch additional patient attributes
      this.emrDB = new EMR();
      serverLog.addMessage(3, 200, "AuditAndComplianceSOI has connected to EMR database.");
    } catch (Throwable e) {
      serverLog.addMessage(1, 200, "Failed to initialize " + this.getClass().getName() + " SOI. " + e.getLocalizedMessage());
    }
  }

  /**
   * This method is called to handle REST requests.
   *
   * @param capabilities the capabilities
   * @param resourceName the resource name
   * @param operationName the operation name
   * @param operationInput the operation input
   * @param outputFormat the output format
   * @param requestProperties the request properties
   * @param responseProperties the response properties
   * @return the response as byte[]
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  @Override
  public byte[] handleRESTRequest(String capabilities, String resourceName, String operationName,
                                  String operationInput, String outputFormat, String requestProperties,
                                  String[] responseProperties) throws IOException, AutomationException {
    String loggedInUser = getLoggedInUserName();

    // Get information about the user from the EMRDB
    Provider provider = emrDB.getProvider(loggedInUser);

    serverLog.addMessage(3, 200,"Request logged in AuditAndComplianceSOI. User: " + loggedInUser + ", Operation: " +
            operationName + ", Operation Input: " + processOperationInput(operationInput) + " Resource: " + resourceName + ", Capabilities = " + capabilities);

    // Find the correct delegate to forward the request too
    IRESTRequestHandler restRequestHandler = soiHelper.findRestRequestHandlerDelegate(so);
    if (restRequestHandler != null) {
      // Delegate to map/feature service for processing original request.
      byte[] response = restRequestHandler.handleRESTRequest(capabilities, resourceName, operationName,
          operationInput, outputFormat, requestProperties, responseProperties);

      serverLog.addMessage(3, 200,"Request logged in AuditAndComplianceSOI. User: " + loggedInUser + ", Response: " + new String(response));

      // Now that we have a map/feature response, we can filter/censor the response before it goes to the client
      if(resourceName.equalsIgnoreCase("") && operationName.equalsIgnoreCase("")) {
        // Root resource
        response = handleRootResource(response, provider);
      } else if(resourceName.contains("layers")) {
        if(operationName.equalsIgnoreCase("query")) {
          // Looks up EMR and write audit logs and integrates additional attributes
          response = enrichQueryResponseWithEMR(response, resourceName, operationInput, provider);
        } else {
          // Filters the attributes based on logged in user
          response = handleLayerResource(response, provider);
        }
      }
      return response;
    }

    return null;
  }

  /**
   * This method is called to handle SOAP requests.
   *
   * @param capabilities the capabilities
   * @param request the request
   * @return the response as String
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  @Override
  public String handleStringRequest(String capabilities, String request) throws IOException,
      AutomationException {
    // Log message with server
    serverLog.addMessage(3, 200,
        "Request received in Sample Object Interceptor for handleStringRequest");

    /*
     * Add code to manipulate SOAP requests here
     */

    // Find the correct delegate to forward the request too
    IRequestHandler requestHandler = soiHelper.findRequestHandlerDelegate(so);
    if (requestHandler != null) {
      // Return the response
      return requestHandler.handleStringRequest(capabilities, request);
    }

    return null;
  }

  /**
   * This method is called by SOAP handler to handle OGC requests.
   *
   * @param httpMethod
   * @param requestURL the request URL
   * @param queryString the query string
   * @param capabilities the capabilities
   * @param requestData the request data
   * @param responseContentType the response content type
   * @param respDataType the response data type
   * @return the response as byte[]
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  @Override
  public byte[] handleStringWebRequest(int httpMethod, String requestURL, String queryString,
                                       String capabilities, String requestData, String[] responseContentType, int[] respDataType)
      throws IOException, AutomationException {
    serverLog.addMessage(3, 200,
        "Request received in Sample Object Interceptor for handleStringWebRequest");

    /*
     * Add code to manipulate OGC (WMS, WFC, WCS etc) requests here
     */

    IWebRequestHandler webRequestHandler = soiHelper.findWebRequestHandlerDelegate(so);
    if (webRequestHandler != null) {
      return webRequestHandler.handleStringWebRequest(httpMethod, requestURL, queryString,
          capabilities, requestData, responseContentType, respDataType);
    }

    return null;
  }

  /**
   * This method is called to handle binary requests from desktop.
   *
   * @param capabilities the capabilities
   * @param request
   * @return the response as byte[]
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  @Override
  public byte[] handleBinaryRequest2(String capabilities, byte[] request) throws IOException,
      AutomationException {
    serverLog.addMessage(3, 200,
        "Request received in Sample Object Interceptor for handleBinaryRequest2");

    /*
     * Add code to manipulate Binary requests from desktop here
     */

    IRequestHandler2 requestHandler = soiHelper.findRequestHandler2Delegate(so);
    if (requestHandler != null) {
      return requestHandler.handleBinaryRequest2(capabilities, request);
    }

    return null;
  }

  /**
   * Return the logged in user's user name.
   *
   * @return
   */
  private String getLoggedInUserName() {
    try {
      /*
       * Get the user information.
       */
      String userName = ServerUtilities.getServerUserInfo().getName();

      if(userName.isEmpty()) {
        return new String("Anonymous User");
      }
      return userName;
    } catch (Exception ignore) {
    }

    return new String("Anonymous User");
  }

  /**
   * Get bbox from operationInput
   *
   * @param operationInput
   * @return
   */
  private String processOperationInput(String operationInput) {
    try {
      return "bbox = " + new JSONObject(operationInput).getString("bbox");
    } catch (Exception ignore) {
    }
    return new String ("No input parameters");
  }

  /**
   * This method is called to handle schema requests for custom SOE's.
   *
   * @return the schema as String
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  @Override
  public String getSchema() throws IOException, AutomationException {
    serverLog.addMessage(3, 200, "Request received in Sample Object Interceptor for getSchema");

    /*
     * Add code to manipulate schema requests here
     */

    IRESTRequestHandler restRequestHandler = soiHelper.findRestRequestHandlerDelegate(so);
    if (restRequestHandler != null) {
      return restRequestHandler.getSchema();
    }

    return null;
  }

  /**
   * This method is called to handle binary requests from desktop. It calls the
   * <code>handleBinaryRequest2</code> method with capabilities equal to null.
   *
   * @param request
   * @return the response as the byte[]
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  @Override
  public byte[] handleBinaryRequest(byte[] request) throws IOException, AutomationException {
    serverLog.addMessage(3, 200,
        "Request received in Sample Object Interceptor for handleBinaryRequest");

    /*
     * Add code to manipulate Binary requests from desktop here
     */

    IRequestHandler requestHandler = soiHelper.findRequestHandlerDelegate(so);
    if (requestHandler != null) {
      return requestHandler.handleBinaryRequest(request);
    }

    return null;
  }

  /**
   * shutdown() is called once when the Server Object's context is being shut down and is about to
   * go away.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws AutomationException the automation exception
   */
  public void shutdown() throws IOException, AutomationException {
    /*
     * The SOE should release its reference on the Server Object Helper.
     */
    this.serverLog.addMessage(3, 200, "Shutting down " + this.getClass().getName() + " SOI.");
    this.serverLog = null;
  }

  /**
   * Returns the ArcGIS home directory path.
   *
   * @return
   * @throws Exception
   */
  private String getArcGISHomeDir() throws IOException {
    String arcgisHome = null;
    /* Not found in env, check system property */
    if (System.getProperty(ARCGISHOME_ENV) != null) {
      arcgisHome = System.getProperty(ARCGISHOME_ENV);
    }
    if(arcgisHome == null) {
      /* To make env lookup case insensitive */
      Map<String, String> envs = System.getenv();
      for (String envName : envs.keySet()) {
        if (envName.equalsIgnoreCase(ARCGISHOME_ENV)) {
          arcgisHome = envs.get(envName);
        }
      }
    }
    if(arcgisHome != null && !arcgisHome.endsWith(File.separator)) {
      arcgisHome += File.separator;
    }
    return arcgisHome;
  }

  /**
   * This method alters the attributes/fields in a layer based on role of the user.
   * @param responseFromGDB
   * @param provider
   * @return
   */
  private byte[] handleRootResource(byte[] responseFromGDB, Provider provider) throws AutomationException, IOException {
    try {
      serverLog.addMessage(3, 200,"AuditAndComplianceSOI. In handleRootResource");

      // Load as JSON
      JSONObject response  = new JSONObject(new String(responseFromGDB));

      serverLog.addMessage(3, 200,"AuditAndComplianceSOI. Provider:" + provider.getFirstName() + " " + provider.getLastName() + ", Provider type: " + provider.getProviderType());

      // For MapServer root resource
      JSONArray rootResources = response.getJSONArray("resources");

      for(int i=0; i<rootResources.length(); i++) {
        JSONObject layersResource = rootResources.getJSONObject(i);
        if(!layersResource.getString("name").equalsIgnoreCase("layers")) {
          continue;
        }

        JSONArray layersResources = layersResource.getJSONArray("resources");
        for(int j=0; j<layersResources.length(); j++) {
          JSONObject layer = layersResources.getJSONObject(j);
          JSONObject layerContents = layer.getJSONObject("contents");

          // Delegate to layer handling
          byte[] layerContentsBytes = handleLayerResource(layerContents.toString().getBytes(), provider);
          // Update the layer content based on layer handling
          layer.put("contents", new JSONObject(new String(layerContentsBytes)));
        }
      }

      // Return response
      serverLog.addMessage(3, 200,"AuditAndComplianceSOI. handleRootResource. Just before returning response: " + response.toString());
      return response.toString().getBytes();
    } catch (Exception e) {
      serverLog.addMessage(2, 200, "Failed in handleRootResource AuditAndComplianceSOI. " + e.getLocalizedMessage());
    }

    return responseFromGDB;
  }

  /**
   * Handles a layer response.
   * @param layerResource
   * @param provider
   * @return
   * @throws AutomationException
   * @throws IOException
   */
  private byte[] handleLayerResource(byte[] layerResource, Provider provider) throws AutomationException, IOException {
    if(provider == null || provider.getProviderType().toLowerCase().contains("GIS Analyst")) {
      // We provide minimal set of attributes directly from the GIS
      serverLog.addMessage(3, 200,"AuditAndComplianceSOI. Provider is null or type is GIS Analyst.");
      return layerResource;
    }

    JSONObject layerContents = new JSONObject(new String(layerResource));

    // Test the layer that we are processing
    if(layerContents.has("id") && layerContents.getInt("id") != 0) {
      return layerResource;
    }
    try {
      if(layerContents.getString("name").equalsIgnoreCase("Patients")) {
        JSONArray fields = layerContents.getJSONArray("fields");

        // Add the additional fields from the EMRDB that are useful to the provider
        serverLog.addMessage(3, 200,"AuditAndComplianceSOI. Adding additional attributes from the Patients EMRDB tables.");
        JSONObject firstName = new JSONObject();
        firstName.put("name", "firstname");
        firstName.put("type", "esriFieldTypeString");
        firstName.put("alias", "First Name");
        firstName.put("length", 50);
        firstName.put("domain", JSONObject.NULL);
        firstName.put("editable", false);
        firstName.put("nullable", false);
        firstName.put("defaultValue", JSONObject.NULL);
        firstName.put("modelName", "First Name");
        fields.put(firstName);

        JSONObject lastName = new JSONObject();
        lastName.put("name", "lastname");
        lastName.put("type", "esriFieldTypeString");
        lastName.put("alias", "Last Name");
        lastName.put("length", 50);
        lastName.put("domain", JSONObject.NULL);
        lastName.put("editable", false);
        lastName.put("nullable", false);
        lastName.put("defaultValue", JSONObject.NULL);
        lastName.put("modelName", "Last Name");
        fields.put(lastName);

        JSONObject sex = new JSONObject();
        sex.put("name", "sex");
        sex.put("type", "esriFieldTypeString");
        sex.put("alias", "Sex");
        sex.put("length", 1);
        sex.put("domain", JSONObject.NULL);
        sex.put("editable", false);
        sex.put("nullable", false);
        sex.put("defaultValue", JSONObject.NULL);
        sex.put("modelName", "Sex");
        fields.put(sex);

        JSONObject nextApt = new JSONObject();
        nextApt.put("name", "nextAppointment");
        nextApt.put("type", "esriFieldTypeString");
        nextApt.put("alias", "Next Appointment");
        nextApt.put("length", 50);
        nextApt.put("domain", JSONObject.NULL);
        nextApt.put("editable", false);
        nextApt.put("nullable", false);
        nextApt.put("defaultValue", JSONObject.NULL);
        nextApt.put("modelName", "Next Appointment");
        fields.put(nextApt);

        JSONObject nextAptLoc = new JSONObject();
        nextAptLoc.put("name", "nextAppointmentLoc");
        nextAptLoc.put("type", "esriFieldTypeString");
        nextAptLoc.put("alias", "Appointment Location");
        nextAptLoc.put("length", 50);
        nextAptLoc.put("domain", JSONObject.NULL);
        nextAptLoc.put("editable", false);
        nextAptLoc.put("nullable", false);
        nextAptLoc.put("defaultValue", JSONObject.NULL);
        nextAptLoc.put("modelName", "Appointment Location");
        fields.put(nextAptLoc);

        serverLog.addMessage(3, 200,"AuditAndComplianceSOI. Adding additional attributes to templates from the Patients EMRDB tables.");
        JSONArray templates = layerContents.optJSONArray("templates");
        if(templates != null) {
          for(int i=0; i<templates.length(); i++) {
            JSONObject template = templates.getJSONObject(i);
            if(template.getString("name").equalsIgnoreCase("Patients")) {
              JSONObject prototype = template.getJSONObject("prototype");
              JSONObject attributes = prototype.getJSONObject("attributes");
              attributes.put("firstname", JSONObject.NULL);
              attributes.put("lastname", JSONObject.NULL);
              attributes.put("sex", JSONObject.NULL);
              attributes.put("nextAppointment", JSONObject.NULL);
              attributes.put("nextAppointmentLoc", JSONObject.NULL);
            }
          }
        }
      }

      serverLog.addMessage(3, 200,"AuditAndComplianceSOI. handleLayerResource. Just before returning response: " + layerContents.toString());
      return layerContents.toString().getBytes();
    } catch (Exception e) {
      serverLog.addMessage(2, 200, "Failed to handleLayerResource in AuditAndComplianceSOI. " + e.getLocalizedMessage());
    }

    // Send the received response if we have not processed anything
    return layerResource;
  }

  /**
   * Handle query
   *
   * @param responseFromGDB
   * @param provider
   * @return
   * @throws AutomationException
   * @throws IOException
   */
  private byte[] enrichQueryResponseWithEMR(byte[] responseFromGDB, String resourceName, String operationInput, Provider provider) throws AutomationException, IOException {
    // Perform authorization and only allow EMR attributes for authorized users only
    if (provider == null || provider.getProviderType().toLowerCase().contains("gis analyst")) {
      // We provide minimal set of attributes directly from the GIS
      serverLog.addMessage(3, 200, "AuditAndComplianceSOI. Provider is null or type is GIS Analyst.");
      return responseFromGDB;
    }

    // We are only manipulating query on patients layer which is layer 0
    if (!resourceName.contains("layers/0")) {
      return responseFromGDB;
    }

    JSONObject queryResponse = new JSONObject(new String(responseFromGDB));
    try {
      // Update the fields
      JSONArray fields = queryResponse.optJSONArray("fields");
      if(fields == null) {
        // No fields nothing to do here
        return responseFromGDB;
      }

      // Add the additional fields from the EMRDB that are useful to the provider
      serverLog.addMessage(3, 200, "AuditAndComplianceSOI. Adding additional attributes from the Patients EMRDB tables.");
      JSONObject firstName = new JSONObject();
      firstName.put("name", "firstname");
      firstName.put("type", "esriFieldTypeString");
      firstName.put("alias", "First Name");
      fields.put(firstName);

      JSONObject lastName = new JSONObject();
      lastName.put("name", "lastname");
      lastName.put("type", "esriFieldTypeString");
      lastName.put("alias", "Last Name");
      fields.put(lastName);

      JSONObject sex = new JSONObject();
      sex.put("name", "sex");
      sex.put("type", "esriFieldTypeString");
      sex.put("alias", "Sex");
      fields.put(sex);

      JSONObject nextApt = new JSONObject();
      nextApt.put("name", "nextAppointment");
      nextApt.put("type", "esriFieldTypeString");
      nextApt.put("alias", "Next Appointment");
      fields.put(nextApt);

      JSONObject nextAptLoc = new JSONObject();
      nextAptLoc.put("name", "nextAppointmentLoc");
      nextAptLoc.put("type", "esriFieldTypeString");
      nextAptLoc.put("alias", "Appointment Location");
      fields.put(nextAptLoc);

      // Update the attributes
      JSONArray features = queryResponse.getJSONArray("features");
      for(int i=0; i<features.length(); i++) {
        JSONObject feature = features.getJSONObject(i);
        JSONObject attributes = feature.getJSONObject("attributes");

        // Get patient id
        serverLog.addMessage(3, 200, "AuditAndComplianceSOI. enrichQueryResponseWithEMR. Updating patient attributes from EMRDB");
        int patientId = attributes.getInt("patientid");

        // Populate the values of the inserted attributes from the EMRDB
        Patient patient = emrDB.getPatient(String.valueOf(patientId));
        if(patient == null) {
          patient = new Patient();
          patient.setFirstName("John");
          patient.setLastName("Doe");
          patient.setSex("m");
        }

        // Get patient's appointments
        serverLog.addMessage(3, 200, "AuditAndComplianceSOI. enrichQueryResponseWithEMR. Updating patient appointments from EMRDB");
        List<Appointment> appointmentList = emrDB.getPatientAppointments(String.valueOf(patientId));
        String patientNextApt = "None";
        String patientNextAptLoc = "N/A";
        if(!appointmentList.isEmpty()) {
          patientNextApt = appointmentList.get(0).getAccessTime().toString();
          patientNextAptLoc = appointmentList.get(0).getLocation();
        }

        // Add additional patient attributes
        attributes.put("firstname", patient.getFirstName());
        attributes.put("lastname", patient.getLastName());
        attributes.put("sex", patient.getSex());
        attributes.put("nextAppointment", patientNextApt);
        attributes.put("nextAppointmentLoc", patientNextAptLoc);

        // Record that patient data was accessed in the database
        AuditLogRecord alr = new AuditLogRecord();
        alr.setPatientId(String.valueOf(patientId));
        alr.setProvider(provider.getProviderId());
        alr.setProviderType(provider.getProviderType());
        alr.setAccessTime(new Date());
        alr.setLocation(provider.getLastLoginLocation());

        // Store audit record to EMRDB
        serverLog.addMessage(3, 200, "AuditAndComplianceSOI. enrichQueryResponseWithEMR. Writing audit log record to EMRDB.");
        try {
          emrDB.storeAuditLog(alr);
        } catch (Exception e) {
          serverLog.addMessage(2, 200, "Failed to write audit log record in AuditAndComplianceSOI. " + e.getLocalizedMessage());
        }
      }

      serverLog.addMessage(3, 200, "AuditAndComplianceSOI. enrichQueryResponseWithEMR. Just before returning response: " + queryResponse.toString());
      return queryResponse.toString().getBytes();
    } catch (Exception e) {
      serverLog.addMessage(2, 200, "Failed to enrichQueryResponseWithEMR in AuditAndComplianceSOI. " + e.getLocalizedMessage());
    }

    return responseFromGDB;

  }

  public AuditAndComplianceSOI() {
    System.out.println("Instance of " + this.getClass().getName() + " was constructed.");
  }
}
