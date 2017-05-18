package ru.ispras.atr.rank

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.functions.desc
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.FeatureConfig

/**
  * Creates Spark Dataframe for term candidates and their scores (feature values),
  * then appends new column containing estimation of term probability for each candidate
  */
abstract class SparkTermCandidatesWeighter(docsToShow:Int) extends TermCandidatesWeighter {
  val termDFName = "Term"

  def allFeatures: Seq[FeatureConfig]

  def convert2FeatureSpace(candidates: Seq[TermCandidate], dataset: DSDataset):Seq[Seq[Double]] = {
    val resByFeatures: Seq[Seq[Double]] = allFeatures.map(f => {
      //iterate by features first, because it lets to estimate time per feature and (maybe) it is faster due to caching
      log.info(s"Initializing feature ${f.id}...")
      val featureComputer = f.build(candidates, dataset)
      log.info(s"Computing feature ${f.id}...")
      featureComputer.compute(candidates)
    })
    log.info(s"${allFeatures.size} features have been computed")
    resByFeatures.transpose
    }

  def convertToDF(termNames: Seq[String], featureNames: Seq[String], resByTerms: Seq[Seq[Double]]): DataFrame = {
    val header = StructField(termDFName, StringType) +: featureNames.map(f => StructField(f, DoubleType))
    val schema = StructType(header)
    val rows = termNames.zip(resByTerms).map(a => Row.fromSeq(a._1 +: a._2))
    val rowsRDD: RDD[Row] = SparkConfigs.sc.parallelize(rows)
    val df = SparkConfigs.sqlc.createDataFrame(rowsRDD, schema)
    df
  }

  def weightAndSort(candidates: Seq[TermCandidate], dataset: DSDataset): Iterable[(String, Double)] = {
    val featureValues = convert2FeatureSpace(candidates, dataset)
    val initDF = convertToDF(candidates.map(_.verboseRepr(docsToShow)), allFeatures.map(_.id), featureValues)
    val weightedDF = weight(initDF)
    val termNamesDF = weightedDF.select(termDFName,id).sort(desc(id))
    val weightColId: String = id //for serialization
    val termColId: String = termDFName
    val terms = termNamesDF.rdd.map(r => (r.getAs[String](termColId), r.getAs[Double](weightColId))).collect()
    terms
  }

  def weight(df: DataFrame) : DataFrame
}

object SparkConfigs {
  val sparkConf = new SparkConf()
    .setAppName("ATR Evaluation System")
    .setMaster("local[16]")
    .set("spark.driver.memory", "1g")
  val sc = new SparkContext(sparkConf)
  val sqlc = new HiveContext(sc)
}