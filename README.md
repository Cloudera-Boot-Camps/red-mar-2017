# red-mar-2017

1.  Data Import - Sqoop
Created sqoop jobs to import data from an Oracle database.  There are 4 tables to import:

ADMIN.MEASUREMENTS -- smaller volume
ADMIN.ASTROPHYSICISTS-- smaller volume
ADMIN.DETECTORS --smaller volume
ADMIN.GALAXIES -- around 500 million records(bigger table)

Import manually on command line
Data was imported using hive import with parquet format for all tables except measurement as its a large volume table so used --direct parameter while sqoop import and Used '|' delimiter while importing data, but issue with hive import with parquet format is it creates all column data type as string and we can not get the original schema data types.
So changed process and just imported tables into HDFS directories and created external tables on top of HDFS location with correct data types.
Created other set of tables with parquest storage and inserted data into parquest tables from text tables.
with more number of mappers in sqoop command its mandatory to mention --split-by argument.


Import using - Oozie
Oozie was used to automate and schedule the import of the data.  Since all sqoop jobs were very similar we created a single workflow that contains a paremeterized job that runs a sqoop command and then a hive script to load the tables.
There is another workflow load and transform which create all parquet tables , insert data into parquet tables by casting the data types , created views for joining tables and for transformations and finally to save that materialises the view into final table.
Used workflows, subworkflows and coordinators
 Workflows
SqoopImport
    1. Sqoop job to copy data to staging directory and subworkflows executed parallely using fork to sqoop import 3 dimensional tables.
    1. Hive script to create hive external tables.
 Load and Transform forkflow for creating parquet and partitionedtables(measurement) and joins and transformations
 and materialize the final view

 Streaming
Build a sample streaming system.  Uses a data generator and then
gen-> Flume -> HDFS
gen-> Flume -> HBASE
1. gen -> Flume -> Kafka -> Spark Streaming -> Kudu

### Setup
#### Generator
[generator](generator/)
```mvn package```
From local
```scp -i *keyfile* /target/bootcamp-0.0.1-SNAPSHOT.jar ec2-user@ec2-35-163-24-37.us-west-2.compute.amazonaws.com:/home/ec2-user/```

Run data generator on the remote host - we chose to run on the same machine where the flume agent was installed.
```
java -cp bootcamp-0.0.1-SNAPSHOT.jar com.cloudera.fce.bootcamp.MeasurementGenerator localhost 9999
```

All Configurations used for Flume to HDFS/HBASE/KAFKA are mentioned under Flume.

Issue Faced: Flume memory channel buffer size was so tight that the data was not able to fit and spark-streaming program was not able to get current streaming data from Kafka so increased the buffer size from 1 MB to 50 MB and was able to resolve the issue. Details are mentioned under Flume directory.
