Steps followed:
1. Started the Java Generator (generator.md)
2. Tested the data generation through Netcat command --> Working fine (netcat.md)
3. Configured Flume agent to refer source as Netcat and Sink as HDFS --> Working fine (flume_hdfs_config.md)
4. Configured Flume agent to refer source as Netcat and Sink as Hbase --> Working fine (flume_hbase_config.md)
    a. Created Hbase table with one column
    b. Started the flume agent
    c. tested the Hbase table to confirm the data loading
5. Configured Flume agent to refer source as Netcat and Sink as Kafka --> Working fine (flume_kafka_config.md)
    a. Created Kafka topic called measurements_event
    b. Tested successfully simple producer and consumer command 
    c. Started Flume agent
    d. Tested successfully for the data load into Kafka.
6. Flume, Kafka and Spark Integration
Issues: Stream job was not able to pick the data. We then realized that, Kafka consumer is stopping after consuming few set of records.
Changed the flume parameters to increase memory to 50MB and batch size as 200, which made sure that Kafka job does not stop..Now Kafka consumer reading messages continuously.



