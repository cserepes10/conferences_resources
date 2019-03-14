# soepackagermavenplugin
Maven plugin implementation for packaging ArcGIS Server SOEs and SOIs.

# Prerequisites

1. OpenJDK 11.0

2. Apache Maven 3.0

# Building the source

1. From within this folder, run the following command `mvn clean install`

3. This will create a maven plugin JAR file that can package your SOEs.

# Using this plugin

1. For an example on using this plugin please refer to the following pom file.

`..\audit_enrichment_hipaa_soi\pom.xml`

```
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.0</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <version>3.1.1</version>
                <executions>
                    <execution>
                        <id>copy-dependencies</id>
                        <phase>package</phase>
                        <goals>
                            <goal>copy-dependencies</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.build.directory}/dependencies</outputDirectory>
                            <overWriteReleases>false</overWriteReleases>
                            <overWriteSnapshots>false</overWriteSnapshots>
                            <overWriteIfNewer>true</overWriteIfNewer>
                            <excludeScope>provided</excludeScope>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>com.esri.arcgis</groupId>
                <artifactId>soe-packager-maven-plugin</artifactId>
                <version>1.0.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>package</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <name>AuditAndComplianceSOI</name>
                    <id>{e6809594-2ff9-11e9-b210-d663bd873d93}</id>
                    <displayName>AuditAndComplianceSOI</displayName>
                    <description>Generates logs for patient data access and enriches patient layers.</description>
                    <version>1.0.0</version>
                    <supportsREST>true</supportsREST>
                    <author>Shreyas Shinde</author>
                    <company>Esri</company>
                    <soeClassName>com.esri.arcgis.demo.AuditAndComplianceSOI</soeClassName>
                    <supportsInterceptor>true</supportsInterceptor>
                    <dependenciesDir>dependencies</dependenciesDir>
                </configuration>
            </plugin>
        </plugins>
    </build>
```


