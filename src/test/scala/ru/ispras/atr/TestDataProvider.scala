package ru.ispras.atr

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}


object TestDataProvider {

  val sparkConf = new SparkConf()
    .setAppName("ATR_unit_test")
    .setMaster("local[16]")
    .set("spark.driver.memory", "1g")
  val sc = new SparkContext(sparkConf)
  val sqlContext = new HiveContext(TestDataProvider.sc)

  def getNewSC = new SparkContext(sparkConf)

}
