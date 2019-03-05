# audit_enrichment_hipaa_soi
A repository contains demo SOI application for Dev Summit 2019.

# Prerequisites

1. OpenJDK 11.0

2. Apache Maven 3.0

3. PostgreSQL 11.0

# Setup

1. Create a new database in your PostgreSQL 11.0 by name `emrdb`

2. Using PgAdminIII or any other SQL tool run the `src\main\resources\tables.sql`. This will create the necessary tables.

3. Using PgAdminIII or any other SQL tool run the `src\main\resources\data.sql`. This will create the dummy data.

4. Edit `src\main\resources\jdbc-connection.properties` file and update it with your database's connection properties.

# Building the source

1. This project depeneds upon the SOE packaging plugin project ..\soepackagermavenplugin. Please build that project first.

2. Then from within this folder, run the following command `mvn clean install`

3. This will create the file with `.soe` extension in the target folder.

# Upload the SOE to your ArcGIS Server Manager

1. Login into ArcGIS Server Manager using an administrative account.

2. Go to Site > Extensions > Add Extension. Upload the extension `.soe` file.

3. Publish service definition `src\main\resources\PatientsAndHospitalsIEHS.sd` to your Server. This will create a map service with Feature Access capability enabled. Data will be copied to the managed database of your server.

4. Enable interceptor on the `PatientsAndHospitalsIEHS` map service by editing Capabilities > Interceptors. Then restart your service.



