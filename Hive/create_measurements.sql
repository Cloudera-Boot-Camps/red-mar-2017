DROP TABLE IF EXISTS measurements_stage;
create external table measurements_stage (
	measurement_id string, 
	detector_id int, 
	galaxy_id int, 
	astrophysicist_id int, 
	measurement_time double, 
	amplitude_1 double, 
	amplitude_2 double, 
	amplitude_3 double) 
row format delimited fields terminated by '|' 
location '/user/ec2-user/gravity/ADMIN.MEASUREMENTS';
 
