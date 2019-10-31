DROP DATABASE mango;
DROP USER 'mango'@'localhost';
CREATE DATABASE mango;
CREATE USER 'mango'@'localhost' IDENTIFIED BY 'mango';
GRANT ALL ON mango.* TO 'mango'@'localhost';
