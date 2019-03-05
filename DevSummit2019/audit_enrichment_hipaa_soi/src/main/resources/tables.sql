
DROP TABLE IF EXISTS patients CASCADE;
DROP TABLE IF EXISTS providers CASCADE;
DROP TABLE IF EXISTS facilities CASCADE;
DROP TABLE IF EXISTS medical_history;
DROP TABLE IF EXISTS medical_encounters;
DROP TABLE IF EXISTS appointments;
DROP TABLE IF EXISTS audit_logs;


CREATE TABLE patients
(
  patient_id varchar(40) NOT NULL,
  first_name varchar(50),
  last_name varchar(50),
  ssn varchar(12) NOT NULL,
  dob date,
  sex varchar(1),
  race varchar(20),
  religion varchar(20),
  occupation varchar(50),
  house_number integer,
  street varchar(50),
  city varchar(50),
  state varchar(2),
  zip integer,
  contact_number varchar(20),
  PRIMARY KEY(patient_id)
);

CREATE TABLE providers
(
  provider_id varchar(40) NOT NULL,
  first_name varchar(50),
  last_name varchar(50),
  ssn varchar(12) NOT NULL,
  dob date,
  sex varchar(1),
  provider_type varchar(50),
  credentials varchar(50),
  license varchar(50),
  last_login timestamp,
  last_login_location varchar(50),
  PRIMARY KEY(provider_id)
);

CREATE TABLE facilities
(
  facility_id varchar(40) NOT NULL,
  name varchar(100),
  street varchar(50),
  city varchar(50),
  state varchar(2),
  zip integer,
  contact_number varchar(20),
  PRIMARY KEY(facility_id)
);

CREATE TABLE medical_history
(
  patient_id varchar(40) REFERENCES patients(patient_id),
  allergies varchar(100),
  habits varchar(100),
  medication varchar(100),
  family_history varchar(100),
  obstetric_history varchar(100),
  surgical_history varchar(100),
  immunization_history varchar(100),
  development_history varchar(100),
  social_history varchar(100),
  last_updated timestamp
);

CREATE TABLE medical_encounters
(
  patient_id varchar(40) REFERENCES patients(patient_id),
  provider varchar(100) REFERENCES providers(provider_id),
  chief_complaint varchar(100),
  last_updated timestamp,
  history varchar(100),
  physical varchar(100),
  assessment varchar(100),
  plan varchar(100),
  orders varchar(100),
  prescriptions varchar(100),
  test_results varchar(100),
  progress_notes varchar(100)
);

CREATE TABLE appointments
(
  patient_id varchar(40) REFERENCES patients(patient_id),
  apt_date_time timestamp NOT NULL,
  location varchar(100),
  provider varchar(100) REFERENCES providers(provider_id)
);


CREATE TABLE audit_logs
(
  patient_id varchar(40) REFERENCES patients(patient_id),
  provider varchar(100) REFERENCES providers(provider_id),
  provider_type varchar(50),
  access_time timestamp,
  location varchar(20)
);



