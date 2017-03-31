export HIVE_SERVER2=ip-172-31-15-224.us-west-2.compute.internal
export KAFKA_BROKER=ip-172-31-11-228.us-west-2.compute.internal:9092
export TOPIC=measurements_event
export IMPALAD=ip-172-31-7-20.us-west-2.compute.internal

hbase shell << EOF
disable 'measurements'
drop 'measurements'
create 'measurements', 'cf1'
quit
EOF

beeline -u jdbc:hive2://${HIVE_SERVER2}:10000/default << EOF
drop table if exists measurements_hbase;
create external table measurements_hbase(
        measurement_id string,
        detector_id int,
        galaxy_id int,
        astrophysicist_id int,
        measurement_time double,
        amplitude_1 double,
        amplitude_2 double,
        amplitude_3 double)
STORED BY 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
WITH SERDEPROPERTIES (
"hbase.columns.mapping" =
":key,
cf1:detector_id,
cf1:galaxy_id,
cf1:astrophysicist_id,
cf1:measurement_time,
cf1:amplitude_1,
cf1:amplitude_2,
cf1:amplitude_3" )
TBLPROPERTIES("hbase.table.name" = "measurements");
EOF

spark-submit --class scenarios.StreamingKafkaHbase target/SparkStreamingScenarios.jar  ${KAFKA_BROKER} ${TOPIC}

Run this after
# impala-shell -i ${IMPALAD} -q "select * from measurements_hbase limit 100"
