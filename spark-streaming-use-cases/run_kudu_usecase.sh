export KAFKA_BROKER=ip-172-31-11-228.us-west-2.compute.internal:9092
export TOPIC=measurements_event
export IMPALAD=ip-172-31-7-20.us-west-2.compute.internal
export KUDU_MASTER=ip-172-31-15-224.us-west-2.compute.internal

impala-shell -i ${IMPALAD} << EOF
DROP TABLE measurements_kudu_impala;
CREATE EXTERNAL TABLE measurements_kudu_impala
STORED AS KUDU
TBLPROPERTIES (
  'kudu.table_name' = 'measurements_kudu'
);
EOF

spark-submit --class scenarios.StreamingKafkaKudu target/SparkStreamingScenarios.jar  ${KAFKA_BROKER} ${TOPIC} ${KUDU_MASTER}

Run this after
# impala-shell -i ${IMPALAD} -q "select * from measurements_kudu_impala limit 100"
