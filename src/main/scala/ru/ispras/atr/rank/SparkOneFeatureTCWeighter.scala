package ru.ispras.atr.rank

import org.apache.spark.sql.DataFrame
import ru.ispras.atr.features.FeatureConfig

/**
  * Ranks by value of one feature.
  *
  * Note that it uses Spark, so initialization may take about 30 seconds.
  */
class SparkOneFeatureTCWeighter(feature: FeatureConfig, docsToShow: Int) extends SparkTermCandidatesWeighter(docsToShow){

  override def id: String = feature.id

  override def allFeatures: Seq[FeatureConfig] = Seq(feature)

  //does nothing
  override def weight(df: DataFrame): DataFrame = df
}

case class SparkOneFeatureTCWeighterConfig(feature: FeatureConfig,
                                           docsToShow: Int = 3) extends TermCandidatesWeighterConfig {
  override def build(): TermCandidatesWeighter = new SparkOneFeatureTCWeighter(feature, docsToShow)
}