CREATE TABLE locations (city VARCHAR(64) NOT NULL, jurisdiction CHAR(2), country CHAR(2) NOT NULL, lat FLOAT NOT NULL, lon FLOAT NOT NULL);
CREATE TABLE jurisdictions (country CHAR(2) NOT NULL, jurisdiction CHAR(2) NOT NULL, name VARCHAR(40) NOT NULL, preferred BOOL);
CREATE TABLE tracking (ip CHAR(15) PRIMARY KEY NOT NULL, total INT UNSIGNED NOT NULL, last TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP);