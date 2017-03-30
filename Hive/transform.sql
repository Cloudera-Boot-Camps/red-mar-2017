SET hive.exec.compress.output=true;
SET mapred.output.compression.codec=org.apache.hadoop.io.compress.SnappyCodec;
SET mapred.output.compression.type=BLOCK;
set hive.exec.dynamic.partition.mode=nonstrict;
set hive.exec.max.dynamic.partitions=150;
set hive.exec.max.dynamic.partitions.pernode=150;

drop table if exists galaxies_final;
create table galaxies_final (
        galaxy_id int,
        galaxy_name string,
        galaxy_type string,
        distance_ly double,
        absolute_magnitude double,
        apparent_magnitude double,
        galaxy_group string)
stored as parquet;

insert into galaxies_final
select
        cast(galaxy_id as int),
        galaxy_name, galaxy_type,
        cast(distance_ly as double),
        cast(absolute_magnitude as double),
        cast(apparent_magnitude as double),
        galaxy_group
from galaxies_stage;

drop table if exists detectors_final;
create table detectors_final (
        detector_id int,
        detector_name string,
        country string,
        latitude double,
        longitude double)
stored as parquet;

insert into detectors_final
select
        cast(detector_id as int),
        detector_name,
        country,
        cast(detector_name as double),
        cast(longitude as double)
from detectors_stage;

drop table if exists astrophysicists_final;
create table astrophysicists_final (
        astrophysicist_id int,
        astrophysicist_name string,
        year_of_birth int,
        nationality string)
stored as parquet;

insert into astrophysicists_final
select
        cast(astrophysicist_id as int),
        astrophysicist_name,
        cast(year_of_birth as int),
        nationality
from astrophysicists_stage;

drop table if exists measurements_final;
create table measurements_final (
        measurement_id string,
        detector_id int,
        astrophysicist_id int,
        measurement_time double,
        amplitude_1 double,
        amplitude_2 double,
        amplitude_3 double)
partitioned by (galaxy_id int)
stored as parquet;

insert into measurements_final
        partition (galaxy_id)
select
        measurement_id,
        cast(detector_id as int),
        cast(astrophysicist_id as int),
        cast(measurement_time as double),
        cast(amplitude_1 as double),
        cast(amplitude_2 as double),
        cast(amplitude_3 as double),
        cast(galaxy_id as int)
from
        measurements_stage;

drop view if exists joined_view_final;
create view joined_view_final as
select
        m.*,
        g.galaxy_name,
        g.galaxy_type,
        g.distance_ly,
        g.absolute_magnitude,
        g.apparent_magnitude,
        g.galaxy_group,
        d.detector_name,
        d.country,
        d.latitude,
        d.longitude,
        a.astrophysicist_name,
        a.year_of_birth,
        a.nationality
from
        measurements_final m
inner join
        galaxies_final g
                on m.galaxy_id = g.galaxy_id
inner join
        detectors_final d
                on m.detector_id = d.detector_id
inner join
        astrophysicists_final a
                on m.astrophysicist_id = a.astrophysicist_id;

drop view if exists detected_view_final;
create view detected_view_final as
select
        *
from
        measurements_final
where
        amplitude_1 > 0.995 and amplitude_3 > 0.995 and amplitude_2 < 0.005;

drop view if exists measurements_view_denormalized;
create view measurements_view_denormalized as
select 
      a.*,
      case  
           when b.measurement_id is null then 'N'
           else	'Y'
      end as detected
from
   joined_view_final a
left join 
   detected_view_final b
on
   a.measurement_id = b.measurement_id;

drop table if exists measurement_physical_final;
create table measurement_physical_final (
        measurement_id string,
        detector_id int,
        astrophysicist_id int,
        measurement_time double,
        amplitude_1 double,
        amplitude_2 double,
        amplitude_3 double,
        galaxy_id  int,
        galaxy_name string,
        galaxy_type string,
        distance_ly double,
        absolute_magnitude double,
        apparent_magnitude double,
        galaxy_group string,
        detector_name string,
        country string,
        latitude double,
        longitude double,
        astrophysicist_name string,
        year_of_birth int,
        nationality string,
        detected string)
STORED AS PARQUET;
insert into measurement_physical_final select * from measurements_view_denormalized;


