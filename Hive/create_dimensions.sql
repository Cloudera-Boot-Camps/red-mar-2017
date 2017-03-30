DROP TABLE IF EXISTS GALAXIES_STAGE;
create external table galaxies_stage (
        galaxy_id int,
        galaxy_name string,
        galaxy_type string,
        distance_ly double,
        absolute_magnitude double,
        apparent_magnitude double,
        galaxy_group string)
        ROW FORMAT DELIMITED FIELDS TERMINATED BY '|'
stored as textfile
location '/user/ec2-user/gravity/ADMIN.GALAXIES';

DROP TABLE IF EXISTS DETECTORS_STAGE;
create external table detectors_stage (
        detector_id string,
        detector_name string,
        country string,
        latitude string,
        longitude string)
ROW FORMAT DELIMITED FIELDS TERMINATED BY '|'
stored as textfile
location '/user/ec2-user/gravity/ADMIN.DETECTORS';

DROP TABLE IF EXISTS ASTROPHYSICISTS_STAGE;
create external table astrophysicists_stage (
        astrophysicist_id string,
        astrophysicist_name string,
        year_of_birth string,
        nationality string)
ROW FORMAT DELIMITED FIELDS TERMINATED BY '|'
stored as textfile
location '/user/ec2-user/gravity/ADMIN.ASTROPHYSICISTS';



