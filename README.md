# Red Team Bootcamp Repository

## Day 1 - Ingest

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

### Notes

We noticed when using hive import into parquet, sqoop will creates all column data type as string. Hence, the original table schema is lost.

Without the direct option, you must specify the --split-by clause in the sqoop import command.

We performed the ETL in Impala initially. It failed on large number of rows. We then opted to use Hive.  Impala is not suited for this kind of workload.

### Issues  

1. Oracle permissions

  The credentials gravity/gravity could not access the Oracle catalog. Could not see the tables.
2. Incorrect URL specified  

  Per standard Oracle JDBC connection strings, we used "/" as a separator beteen host:port and database. It did not like this. We changed to ":", as specified in the instructions.

## Day 2 - Batch Processing

## Workflow Automation - Oozie  
Oozie was used to automate and schedule the import of the data.  Since all sqoop jobs were very similar we created a single workflow that contains a paremeterized job that runs a sqoop command and then a hive script to load the tables.
There is another workflow load and transform which create all parquet tables , insert data into parquet tables by casting the data types , created views for joining tables and for transformations and finally to save that materialises the view into final table. Used workflows, subworkflows and coordinators

### Workflows
Sqoop Import

1. We created a parameterized sqoop workflow that will be used to ingest the dimension tables into HDFS directories.
2. We created a workflow to ingest all Dimension tables. It used sub-workflows based our parameterized sqoop workflow. Our dimension ingest workflow included a hive action to create external tables over the data.
3. We created a specific sqoop workflow for the fact table, ensuring the --direct parameter was specified. To limit the number of records retrieved from the source, we specified --where 'ROWNUM<${ROWS}'.  This workflow included a hive action to create an external table over the data.
4. We then created a super work flow that included the sub-workflows to ingest the dimension data, and also ingest the fact table. The last stage of this action was to generate the views and physicalized version of the data based on Day 1.  Resulting data was stored a parquet, and the large fact tables was partitioned by galaxy_id.

### Notes

1. Impala - string to double, int issues  
We create external tables over the sqoop ingested delimited data, specifying the correct column types (int,double,string).  If you use impala to then load the data into parquet tables, we got type conversion issues if the numeric fields was NULL.  We did not see this issue with Hive.

### Issues

1. Limiting the Measurements ingest

  We iterated over several approaches to minimize the number of rows extracted by sqoop. The initial attempts included removing the --direct and using --query. We ended up using --direct, but noticed we could specify --where with ROWNUM.


## Day 3 - Streaming

### Build a sample streaming system.  

We were provided a simple java program that generated random measurement data. We had to ingest the stream the data in the following scenarios:

  1. Generator -> Flume -> HDFS  
  2. Generator -> Flume -> HBASE  
  3. Generator -> Flume -> Kafka -> Spark Streaming -> HBase
  4. Generator -> Flume -> Kafka -> Spark Streaming -> Kudu


### Setup
#### Generator

The provided generator will generate random data and direct it to a host:port. Flume can receive this data via the default netcat souce.

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
4. Confirm we can receive the data. Ensure Flume is not running! Run netcat on the edge node.
```
ncat -v -l -p 9999
```  


#### Flume Setup  

All Configurations used for Flume to HDFS/HBASE/KAFKA are mentioned under Flume.

### Notes

Kudu has a flat namespace.  You can create a database in Impala to organize the kudu tables, but it would just be exposed to impala. Spark Streaming, etc, will not be privvy to this.  

## Issue Faced  

1. Flume data stalling in Kafka
  Flume memory channel buffer size was so tight that the data was not able to fit and spark-streaming program was not able to get current streaming data from Kafka so increased the buffer size from 1 MB to 50 MB and batch size from 20 to 200 and was able to resolve the issue. For writing to HDFS increased channel capacity to 100.  Details are mentioned under Flume directory.
2. Coding  
  There are multiple ways to load data into HBase - 4 ways.
    a) Hbase puts
    b) Bulk puts using saveAs* style APIs
    c) BulkMutable* HBase APIs.
    d) HBase spark context StreamBulkPuts.
  Each has their own idiosyncrasies. We opted for d).
  d) requires you to set up an HBase configuration obect, and add resources to point to the hbase-site, and hbase-core-site xml files. You need to add these as Path objects not String!!!!!!
