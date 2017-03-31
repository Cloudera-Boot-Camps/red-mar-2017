package scenarios

import kafka.serializer.StringDecoder
import org.apache.hadoop.fs.Path

import org.apache.spark.streaming.kafka._
import org.apache.spark.SparkConf

import org.apache.hadoop.hbase.spark.HBaseContext
import org.apache.spark.SparkContext
import org.apache.hadoop.hbase.{TableName, HBaseConfiguration}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.client.Put
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.Seconds

object StreamingKafkaHbase {

  def main(args: Array[String]) {
    if (args.length < 2) {
      System.exit(1)
    }

    // NOTE: We are making the assumption the the "measurements" hbase table was already created as follows:
    // hbase> create 'measurements', 'cf1'

    val Array(brokers, topics) = args

    val sparkConf = new SparkConf().setAppName("StreamingKafkaHbase")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(2))

    // set up the HBase configuration object
    val conf = HBaseConfiguration.create();
    // note: new Path(), addResource seems to take String objects as well, but will cause the puts to fail!
    conf.addResource(new Path(Measurements.HBASE_CORE_SITE));
    conf.addResource(new Path(Measurements.HBASE_SITE));

    // get our HBase context
    val hbaseContext = new HBaseContext(sc, conf);

    // start streaming from Kafka
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val events = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)

    // as a check, lets dump the events to stdout
    events.print()

    // there's a myriad of was to put HBase data, this is just one way to do it
    hbaseContext.streamBulkPut[(String, String)](events,
      TableName.valueOf(Measurements.HBASE_NAME),
      (putRecord) => {
        val fields = putRecord._2.split(",")
        val measurement_id = fields(Measurements.POS_MEASUREMENTS_ID)
        val detector_id = fields(Measurements.POS_DETECTOR_ID)
        val galaxy_id = fields(Measurements.POS_GALAXY_ID)
        val astrophysicist_id = fields(Measurements.POS_ASTROPHYSICISTS_ID)
        val measurement_time = fields(Measurements.POS_MEASUREMENTS_TIME)
        val amplitude_1 = fields(Measurements.POS_AMPLITUDE_1)
        val amplitude_2 = fields(Measurements.POS_AMPLITUDE_2)
        val amplitude_3 = fields(Measurements.POS_AMPLITUDE_3)
        val anomaly= Measurements.anomalyCheck(amplitude_1.toDouble, amplitude_2.toDouble, amplitude_3.toDouble)

        val put = new Put(Bytes.toBytes(measurement_id))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_GALAXY_ID), Bytes.toBytes(galaxy_id))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_DETECTOR_ID), Bytes.toBytes(detector_id))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_ASTROPHYSICISTS_ID), Bytes.toBytes(astrophysicist_id))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_MEASUREMENTS_TIME), Bytes.toBytes(measurement_time))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_AMPLITUDE_1), Bytes.toBytes(amplitude_1))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_AMPLITUDE_2), Bytes.toBytes(amplitude_2))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_AMPLITUDE_3), Bytes.toBytes(amplitude_3))
        put.addColumn(Bytes.toBytes(Measurements.COL_FAMILY), Bytes.toBytes(Measurements.COL_ANOMALY), Bytes.toBytes(anomaly))
      }
    )

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }
}
