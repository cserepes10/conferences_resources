package com.esri.arcgis;


import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Mojo(name = "package")
public class ArcGISSOEPackagerMojo extends AbstractMojo {

  private static int MAX_BUFFER_LENGTH = 1024;

  @Parameter( property = "id")
  private String id = "{" + UUID.randomUUID().toString() + "}";

  @Parameter( property = "date")
  private Date date = new Date();

  @Parameter( property = "name")
  private String name;

  @Parameter( property = "serverObjectType", defaultValue = "MapServer")
  private String serverObjectType;

  @Parameter( property = "soeClassName")
  private String soeClassName;

  @Parameter(property = "displayName")
  private String displayName;

  @Parameter(property = "description")
  private String description;

  @Parameter(property = "defaultWebCapabilities", defaultValue = "")
  private String defaultWebCapabilities;

  @Parameter(property = "allWebCapabilities", defaultValue = "")
  private String allWebCapabilities;

  @Parameter(property = "supportsSOAP", defaultValue = "false")
  private String supportsSOAP;

  @Parameter(property = "supportsREST", defaultValue = "true")
  private String supportsREST;

  @Parameter(property = "supportsInterceptor", defaultValue = "false")
  private String supportsInterceptor;

  @Parameter(property = "company", defaultValue = "")
  private String company;

  @Parameter(property = "version", defaultValue = "1.0")
  private String version;

  @Parameter(property = "author", defaultValue = "")
  private String author;

  @Parameter(property = "targetServerVersion", defaultValue = "10.1")
  private String targetServerVersion;

  @Parameter(defaultValue="${project}", readonly=true, required=true)
  private MavenProject project;

  @Parameter(property = "dependenciesDir", defaultValue = "dependencies")
  private String dependenciesDir;


  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    getLog().info("ArcGIS SOE Packager goal executed!");
    Map map = getPluginContext();
    System.out.println(map.toString());
    Model model = project.getModel();
    System.out.println("Target dir: " + model.getBuild().getDirectory());

    Build build = model.getBuild();

    // Fully qualified artifact id
    String fqArtifactId = model.getArtifactId() + "-" + model.getVersion();

    // Make the SOE build dir
    File soeBuildDir = Paths.get(build.getDirectory(),  fqArtifactId).toFile();
    getLog().info("Creating the SOE directorty: " + soeBuildDir.getAbsolutePath());
    soeBuildDir.mkdirs();

    File installSoeBuildDir = Paths.get(soeBuildDir.getAbsolutePath(), "Install").toFile();
    installSoeBuildDir.mkdirs();

    // Test that the artifact exists
    File artifactFile = new File(build.getDirectory() + File.separator + fqArtifactId + ".jar");
    if(!artifactFile.exists()) {
      throw new RuntimeException("Missing artifact:" + artifactFile.getAbsolutePath());
    }

    try {
      // Copy the built artifact to the 'Install' dir
      getLog().info("Copying artifact " + artifactFile.getAbsolutePath() + " to " + installSoeBuildDir.getAbsolutePath() + ".");
      Files.copy(Paths.get(artifactFile.getAbsolutePath()), Paths.get(installSoeBuildDir.getAbsolutePath(), fqArtifactId + ".jar"), StandardCopyOption.REPLACE_EXISTING);

      // Copy files into the 'Install' directory
      File targetDepDir = new File(build.getDirectory() + File.separator + dependenciesDir);
      File[] dependencies = targetDepDir.listFiles();
      if(dependencies != null) {
        for(File dep : dependencies) {
          getLog().info("Copying dependency " + dep.getAbsolutePath() + " to " + installSoeBuildDir.getAbsolutePath() + ".");
          Files.copy(Paths.get(dep.getAbsolutePath()), Paths.get(installSoeBuildDir.getAbsolutePath(), dep.getName()));
        }
      }

      // Create and copy the Config.xml to soe directory
      getLog().info("Creating Config.xml.");
      Document configXml = createConfigXml(fqArtifactId + ".jar");
      writeXMLDocument(configXml, Paths.get(soeBuildDir.getAbsolutePath(), "Config.xml").toString());

      // Zip the directory with .soe extension
      getLog().info("Building the .soe file. " + build.getDirectory() + File.separator + fqArtifactId + ".soe");
      zip(soeBuildDir.getAbsolutePath(), build.getDirectory() + File.separator + fqArtifactId + ".soe");
    } catch (Exception e) {
      getLog().error(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a Config.xml file listing out the extensions.
   * @return
   * @throws Exception
   */
  private Document createConfigXml(String library) throws Exception {

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setIgnoringElementContentWhitespace(true);
    Document document = dbf.newDocumentBuilder().newDocument();
    //Root
    Element rootElem = document.createElement("ESRI.Configuration");
    rootElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://schemas.esri.com/Desktop/AddIns");
    rootElem.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    document.appendChild(rootElem);

    // Text content based elements
    appendNewElementIfNotEmpty(document, rootElem,"Company", company);
    appendNewElementIfNotEmpty(document, rootElem,"Description", description);
    appendNewElementIfNotEmpty(document, rootElem,"Version", version);
    appendNewElementIfNotEmpty(document, rootElem,"manifest", "");

    // Targets
    Element targetsElem = document.createElement("Targets");
    Element targetElem = document.createElement("Target");
    targetElem.setAttribute("name", "Server");
    targetElem.setAttribute("version", targetServerVersion);
    targetsElem.appendChild(targetElem);
    rootElem.appendChild(targetsElem);

    appendNewElementIfNotEmpty(document, rootElem,"AddInID", id);
    appendNewElementIfNotEmpty(document, rootElem,"Date", date.toString());
    appendNewElementIfNotEmpty(document, rootElem,"Name", name);

    // AddIn
    Element addInElem = document.createElement("AddIn");
    addInElem.setAttribute("language", "Java");
    rootElem.appendChild(addInElem);
    Element serverObjElem = document.createElement("ServerObjectType");
    addInElem.appendChild(serverObjElem);
    appendNewElementIfNotEmpty(document, serverObjElem, "Name", serverObjectType);
    Element extensionTypesElem = document.createElement("ExtensionTypes");
    serverObjElem.appendChild(extensionTypesElem);
    Element extensionTypeElem = document.createElement("ExtensionType");
    extensionTypeElem.setAttribute("class", soeClassName);
    extensionTypeElem.setAttribute("id", soeClassName);
    extensionTypeElem.setAttribute("library", library);
    extensionTypesElem.appendChild(extensionTypeElem);

    // ExtensionType
    appendNewElementIfNotEmpty(document, extensionTypeElem, "Name", name);
    appendNewElementIfNotEmpty(document, extensionTypeElem, "DisplayName", displayName);
    appendNewElementIfNotEmpty(document, extensionTypeElem, "Description", description);

    // Info elem
    Element infoElem = document.createElement("Info");
    extensionTypeElem.appendChild(infoElem);
    appendNewElementIfNotEmpty(document, infoElem, "DefaultWebCapabilities", defaultWebCapabilities);
    appendNewElementIfNotEmpty(document, infoElem, "AllWebCapabilities", allWebCapabilities);
    appendNewElementIfNotEmpty(document, infoElem, "SupportsSOAP", supportsSOAP);
    appendNewElementIfNotEmpty(document, infoElem, "SupportsREST", supportsREST);
    appendNewElementIfNotEmpty(document, infoElem, "SupportsInterceptor", supportsInterceptor);
    appendNewElementIfNotEmpty(document, infoElem, "hasManagerPropertiesConfigurationPane", "false");

    Element resourcesElem = document.createElement("Resources");
    extensionTypeElem.appendChild(resourcesElem);

    return document;

  }

  /**
   * Write dom to xml.
   *
   * @param xmlDoc
   *          the xml doc
   *
   * @throws Exception
   */
  private void writeXMLDocument(Document xmlDoc, String filename) throws Exception {
    FileOutputStream outFileStream = null;
    try {
      /* Normalize the xml document - merges text nodes */
      xmlDoc.normalize();

      /* Attempt to open the file and lock it */
      outFileStream = new FileOutputStream(filename);
      /*
       * Need to set the indent prop on TransformerFactory - workaround for
       * JDK bug to indent.
       */
      TransformerFactory tFactory = TransformerFactory.newInstance();
      tFactory.setAttribute("indent-number", new Integer(2));

      /* Get transformer */
      Transformer xformer = tFactory.newTransformer();
      xformer.setOutputProperty(OutputKeys.INDENT, "yes");

      /* Wrap the output stream with some outputstreamwriter for indentation */
      Result result = new StreamResult(new OutputStreamWriter(outFileStream, Charset.forName("UTF-8")));
      xformer.transform((Source) new DOMSource(xmlDoc), result);
      outFileStream.flush();
      return;
    } finally {
      if (outFileStream != null) {
        outFileStream.close();
      }
    }
  }/* writeDomToXml() */

  private boolean isEmpty(String value) {
    if(value == null || value.isEmpty()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Zips the input directory path into a zip file path.
   * @param dirPath
   * @param zipFilePath
   * @throws IOException
   */
  public static void zip(String dirPath, String zipFilePath) throws IOException {
    File dir = new File(dirPath);
    ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath));
    String base = "";
    zipFile(dir, zipOutputStream, base);
    zipOutputStream.close();
  }

  /**
   * Zips an input file path into a the zip output stream.
   * @param path
   * @param zipOutputStream
   * @param base
   * @throws IOException
   */
  public static void zipFile(File path, ZipOutputStream zipOutputStream, String base) throws IOException {
    FileInputStream fileInputStream = null;
    try {
      if (path.isDirectory()) {
        File[] childrenCompressedFileList = path.listFiles();
        base = base.length() == 0 ? "" : base + "/";
        // normalize all double slashes to a single slash otherwise
        // we get empty folders in the zip
        base = base.replaceAll("[/\\\\]+", "/");
        // Handling of empty folders
        if(childrenCompressedFileList == null || childrenCompressedFileList.length == 0) {
          zipOutputStream.putNextEntry(new ZipEntry(base));
        } else {
          for (int i = 0; i < childrenCompressedFileList.length; i++) {
            zipFile(childrenCompressedFileList[i], zipOutputStream,
                base + childrenCompressedFileList[i].getName());
          }
        }
      } else {
        if ("".equalsIgnoreCase(base)) {
          base = path.getName();
        }
        zipOutputStream.putNextEntry(new ZipEntry(base));
        fileInputStream = new FileInputStream(path);
        byte[] buf = new byte[MAX_BUFFER_LENGTH];
        int r = 0;
        while ((r = fileInputStream.read(buf)) != -1) {
          zipOutputStream.write(buf, 0, r);
        }
      }
    } finally {
      if (fileInputStream != null) {
        fileInputStream.close();
      }
    }
  }

  /**
   * Appends a new element to the root if the name and textContent are not empty.
   * @param root
   * @param name
   * @param textContent
   */
  private Element appendNewElementIfNotEmpty(Document root, Element parentElem, String name, String textContent) {
    if(!isEmpty(name) && !isEmpty(textContent)) {
      Element element = root.createElement(name);
      element.setTextContent(textContent);
      parentElem.appendChild(element);
      return element;
    } else {
      return null;
    }
  }
}
