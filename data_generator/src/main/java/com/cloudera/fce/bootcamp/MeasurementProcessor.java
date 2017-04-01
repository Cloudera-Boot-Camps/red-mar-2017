package com.cloudera.fce.bootcamp;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.hive.HiveContext;

public class MeasurementProcessor {

    public static void main(String[] args) {

        SparkConf conf = new SparkConf();
        
        JavaSparkContext sc = new JavaSparkContext(conf);
        
        SQLContext sqlc = new HiveContext(sc.sc());
        
        DataFrame derivedTable = sqlc.sql("SELECT * FROM " + args[0] + " limit 10");
                
        derivedTable.write().mode(SaveMode.Overwrite).saveAsTable("spark_output");
        
        sc.close();
        
    }

}
