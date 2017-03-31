package scenarios

import kafka.serializer.StringDecoder
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka._
import org.kududb.client._
import org.kududb.{ColumnSchema, Schema, Type}
import org.kududb.ColumnSchema.ColumnSchemaBuilder
import java.util

object StreamingKafkaKudu {

  // function: writeKuduRec
  // purpose: prepares a kudu record and writes it to a table
  // notes: this will be called per partition, hence the requirements for a session and table object
  def writeKuduRec(session : KuduSession,
                   table : KuduTable,
                   data : String) = {

    // parse the measurement data
    // TODO - there's no checks for NULLS!!!!
    val fields = data.split(",")
    val measurement_id = fields(Measurements.POS_MEASUREMENTS_ID)
    val detector_id = fields(Measurements.POS_DETECTOR_ID).toInt
    val galaxy_id = fields(Measurements.POS_GALAXY_ID).toInt
    val astrophysicist_id = fields(Measurements.POS_ASTROPHYSICISTS_ID).toInt
    val measurement_time = fields(Measurements.POS_MEASUREMENTS_TIME).toDouble
    val amplitude_1 = fields(Measurements.POS_AMPLITUDE_1).toDouble
    val amplitude_2 = fields(Measurements.POS_AMPLITUDE_2).toDouble
    val amplitude_3 = fields(Measurements.POS_AMPLITUDE_3).toDouble
    val anomaly = Measurements.anomalyCheck(amplitude_1,  amplitude_2, amplitude_3)

    val insert = table.newInsert()
    val row = insert.getRow()

    // prepare the row data and insert
    row.addString(Measurements.COL_MEASUREMENTS_ID, measurement_id)
    row.addInt(Measurements.COL_DETECTOR_ID,detector_id)
    row.addInt(Measurements.COL_GALAXY_ID,galaxy_id)
    row.addInt(Measurements.COL_ASTROPHYSICISTS_ID,astrophysicist_id)
    row.addDouble(Measurements.COL_MEASUREMENTS_TIME,measurement_time)
    row.addDouble(Measurements.COL_AMPLITUDE_1,amplitude_1)
    row.addDouble(Measurements.COL_AMPLITUDE_2,amplitude_2)
    row.addDouble(Measurements.COL_AMPLITUDE_3,amplitude_3)
    row.addString(Measurements.COL_ANOMALY,anomaly)

    session.apply(insert)
  }

  def main(args: Array[String]) {

    if (args.length < 3) {
      println("-- usage: broker-list topic-list kudu-master")
      System.exit(1)
    }

    val Array(brokers, topics, kuduMaster) = args

    val sparkConf = new SparkConf().setAppName("StreamingKafkaKudu")
    val sc = new SparkContext(sparkConf)
    val ssc = new StreamingContext(sc, Seconds(2))

    // to optimize the kudu writes, we'll be writing data by partitions
    // each task will need to instatiate its own kudu client with the following info
    val b_tableName = sc.broadcast(Measurements.KUDU_NAME)
    val b_kuduMaster= sc.broadcast(kuduMaster)

    // the job will drop and recreate the kudu table
    val kuduClient = new KuduClient.KuduClientBuilder(b_kuduMaster.value).build()
    if (kuduClient.tableExists(Measurements.KUDU_NAME)) {
      kuduClient.deleteTable(Measurements.KUDU_NAME)
    }

    val columnList = new util.ArrayList[ColumnSchema]()
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_MEASUREMENTS_ID, Type.STRING).key(true).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_DETECTOR_ID, Type.INT32).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_GALAXY_ID, Type.INT32).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_ASTROPHYSICISTS_ID, Type.INT32).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_MEASUREMENTS_TIME, Type.DOUBLE).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_AMPLITUDE_1, Type.DOUBLE).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_AMPLITUDE_2, Type.DOUBLE).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_AMPLITUDE_3, Type.DOUBLE).key(false).build())
    columnList.add(new ColumnSchemaBuilder(Measurements.COL_ANOMALY, Type.STRING).key(false).build())
    val schema = new Schema(columnList)

    val bucket = Measurements.HASH
    val hashColumns = new util.ArrayList[String]()
    hashColumns.add(Measurements.COL_MEASUREMENTS_ID)
    kuduClient.createTable(Measurements.KUDU_NAME, schema,new CreateTableOptions().addHashPartitions(hashColumns,3))
    kuduClient.shutdown

    // stream message from kafka
    val topicsSet = topics.split(",").toSet
    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
    val events = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topicsSet)
    events.print()

    // we've hit our window, lets dump the DStreems
    events.foreachRDD(
      rdd => {
        // perform the kudu writes by partition
        rdd.foreachPartition(part => {

          val kuduClient = new KuduClient.KuduClientBuilder(b_kuduMaster.value).build()
          val session = kuduClient.newSession

          val table = kuduClient.openTable(b_tableName.value)
          part.foreach(x => writeKuduRec(session,table,x._2))
          session.flush()
          session.close
          kuduClient.shutdown()
        }
        )
      })

    ssc.start()
    ssc.awaitTermination()
    ssc.stop()
  }
}
