package ru.ispras.atr.rank

import org.apache.spark.sql.{Column, DataFrame}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{desc, rank, udf}
import ru.ispras.atr.features.FeatureConfig

/**
  * Average of inverted ranks of each feature values.
  *
  * Note that it uses Spark, so initialization may take about 30 seconds.
  *
  * Proposed for ATR in:<br>
  *   Zhang, Z., Iria, J., Brewster, C., & Ciravegna, F. (2008, May).
  *   A comparative evaluation of term recognition algorithms. In LREC.
  */
class VotingTCWeighter(features: Seq[FeatureConfig]) extends SparkTermCandidatesWeighter() {

  override def allFeatures: Seq[FeatureConfig] = features
  /**
    * Computes rank of each feature value (bigger value assumes lesser rank
    * (e.g. values 2,2,3,4 => ranks 2,2,4,1 respectively)
    * NB: Requires HiveContext to be used for df creation
    *
    * @param df dataframe containing feature values in columns
    * @return array of columns corresponding to ranks of each feature
    */
  def getFeatureRankColumns(df: DataFrame): Array[Column] = {
    df.columns.filter(_ != termDFName).map(colName => {
      val w = Window.orderBy(desc(colName))
      rank.over(w).alias("rank" + colName)
    })
  }

  override def weight(df: DataFrame): DataFrame = {
    val featureRanks: Array[Column] = getFeatureRankColumns(df)
    //debug printing
//    val cols: Seq[Column] = df("Term") +: featureRanks.toSeq
//    val dfWithRanks: DataFrame = df.select(cols :_* )
//    dfWithRanks.show()
    val inverseRanks: Array[Column] = featureRanks.map(c => VotingTCWeighter.inverseUDF(c))
    val votingColumn: Column = inverseRanks.reduce((c1, c2) => c1 + c2).alias(id)
    val res = df.select(df(termDFName), votingColumn)
    res
  }
}

object VotingTCWeighter {
  def inverse(rank: Double): Double = 1 / rank
  val inverseUDF = udf(inverse(_:Double))
}

case class VotingTCWeighterConfig(features: Seq[FeatureConfig]) extends TermCandidatesWeighterConfig {
  override def build(): TermCandidatesWeighter = new VotingTCWeighter(features)
}
