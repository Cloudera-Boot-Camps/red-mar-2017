# Red Team Bootcamp Repository

## Ingest Day 1

### RDBMS Ingest - Sqoop

Created sqoop jobs to import data from an Oracle database.  There are 4 tables to import:

  ADMIN.MEASUREMENTS -- smaller volume  
  ADMIN.ASTROPHYSICISTS-- smaller volume    
  ADMIN.DETECTORS --smaller volume  
  ADMIN.GALAXIES -- around 500 million records(bigger table)

##### Dimension Tables - Galaxies, Astrophyicists, Detectors

  These tables are very small.  We were able to ingest these with sqoop without the --direct option. We imported them directly into Hive parquet tables, but noticed that sqoop created all the fields as string.  This was not the case when we ingested these tables as delimited.

  ##### Fact Table - Measurements  

  Given it's size, we used the --direct parameter in Sqoop import and specified '|' delimiter while importing data.  
  We opted to imported tables into an HDFS directories as delimited text and an created external tables over it. We used the source schema.

  We then created a final hive parquet table of the data by creating a managed table in hive, and then performing an INSERT..SELECT statement to load the data into this table.

### Data Processing - Hive

Per the instructions, we then created views on top of these ingested tables.
The first view was an inner join of the FACT table against all the DIMENSIONS. We created a second view that retrieved anomalous readings.

For improved query performance for the end user, we physicalized these views by creating a managed table based on these views, and issued an INSERT..SELECT to load the data. These resultant tables were stored as Parquet.

We also partitioned the tables based on Galaxy ID.  We chose this column as it was easier to see the total number of partitions that would be generated.  To make things simple, we used dynamic partitioning in the final version of these tables.

### Interesting Notes

We noticed when using hive import into parquet, sqoop will creates all column data type as string. Hence, the original table schema is lost.

Without the direct option, you must specify the --split-by clause in the sqoop import command.

### Issues  

1. Oracle permissions

  The credentials gravity/gravity could not access the Oracle catalog. Could not see the tables.
2. Incorrect URL specified  

  Per standard Oracle JDBC connection strings, we used "/" as a separator beteen host:port and database. It did not like this. We changed to ":", as specified in the instructions.

## Batch Processing Day 2

## Workflow Automation - Oozie  
Oozie was used to automate and schedule the import of the data.  Since all sqoop jobs were very similar we created a single workflow that contains a paremeterized job that runs a sqoop command and then a hive script to load the tables.
There is another workflow load and transform which create all parquet tables , insert data into parquet tables by casting the data types , created views for joining tables and for transformations and finally to save that materialises the view into final table. Used workflows, subworkflows and coordinators

### Workflows
Sqoop Import

1. Sqoop job to copy data to staging directory and sub-workflows executed in parallel using fork to sqoop imprt 3 dimensional tables.
2. Hive script to create hive external tables.
Load and Transform forkflow for creating parquet and partitioned tables(measurement) and joins and transformations and materialize the final view

## Streaming

### Build a sample streaming system.  

Uses a data generator and then  

  1. gen-> Flume -> HDFS  
  2. gen-> Flume -> HBASE  
  3. gen -> Flume -> Kafka -> Spark Streaming -> Kudu

### Setup
#### Generator

1. Download the data generator to the edge node  
2. Compile the data generator
```
cd <data generator director>
mvn clean install
```
3. Run the generator on the edge node  
```
java -cp target/bootcamp-0.0.1-SNAPSHOT.jar com.cloudera.fce.bootcamp.MeasurementGenerator localhost 9999
```  

#### Flume Setup  

All Configurations used for Flume to HDFS/HBASE/KAFKA are mentioned under Flume.

## Issue Faced  

1. Sqoop Delimited in Oozie

  If data have '(single quote) then in sqoop then --terminated-by '|' will not work we should have escape char "|"   , in sqoop jdbc connection we tried using jdbc connector/databasename but it didn't work , then mentioned jdbc-connector:databasename.

2. Flume data stalling in Kafka

  Flume memory channel buffer size was so tight that the data was not able to fit and spark-streaming program was not able to get current streaming data from Kafka so increased the buffer size from 1 MB to 50 MB and batch size from 20 to 200 and was able to resolve the issue. For writing to HDFS increased channel capacity to 100.  Details are mentioned under Flume directory.
