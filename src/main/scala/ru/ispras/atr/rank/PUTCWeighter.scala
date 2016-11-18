package ru.ispras.atr.rank

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{desc, rank, udf}
import org.apache.spark.sql.DataFrame
import ru.ispras.atr.features.FeatureConfig
import ru.ispras.pu4spark.{PositiveUnlabeledLearner, PositiveUnlabeledLearnerConfig, ProbabilisticClassifierConfig}

/**
  * Extracts top 50-200 terms by single method (seed method);
  * then computes values for multiple features for all term candidates;
  * learns Positive-Unlabeled (PU) classifier by considering these seed terms as positive instances
  * and all others as unlabeled instances, where each instance is a vector of feature values;
  * and, finally, applies learned classifier to each term candidate,
  * so that the obtained classifier's confidence is a final aggregated value.
  *
  * <br>Based on:
  *   Astrakhantsev, N.: Automatic term acquisition from domain-specific text collection by
  *   using wikipedia. Proceedings of the Institute for System Programming 26(4), 7-20. (2014)
  *
  * Note that it uses Spark, so initialization may take about 30 seconds.
  *
  * @param baseFeature     seed method for extracting seedCount terms that will serve as positives in PU
  * @param seedsCount      count of positives to be extracted
  * @param predictFeatures features for PU learning algorithm
  * @param puLearner       configuration for PU learning algorithm
  */
class PUTCWeighter(baseFeature: FeatureConfig,
                   seedsCount: Int,
                   predictFeatures: Seq[FeatureConfig],
                   puLearner: PositiveUnlabeledLearner) extends SparkTermCandidatesWeighter() {

  val termProbName = "category"
  val srcFeaturesName = "srcFeatures"

  override def allFeatures: Seq[FeatureConfig] = baseFeature +: predictFeatures

  override def weight(df: DataFrame): DataFrame = {
    val w = Window.orderBy(desc(baseFeature.id))
    val termProbCol = new SeedTopBinarizer(seedsCount).binarizeUDF(rank.over(w)).alias(termProbName)

    val dfForML = df.withColumn(termProbName, termProbCol).cache()

    val assembler = new VectorAssembler()
      .setInputCols(df.columns.filter(c => c != termDFName && c != baseFeature.id))
      .setOutputCol(srcFeaturesName)

    val pipeline = new Pipeline().setStages(Array(assembler))
    val preparedDf = pipeline.fit(dfForML).transform(dfForML)

    puLearner.weight(preparedDf, termProbName, srcFeaturesName, id)
  }
}

class SeedTopBinarizer(topCount: Int) extends Serializable {
  def binarize(rank: Double): Int = if (rank <= topCount) 1 else 0
  val binarizeUDF = udf(binarize(_:Double))
}

case class PUTCWeighterConfig
    (baseFeature: FeatureConfig,
     puTopCount: Int = 100,
     predictFeatures: Seq[FeatureConfig],
     puLearnerConfig: PositiveUnlabeledLearnerConfig) extends TermCandidatesWeighterConfig {
  override def build(): PUTCWeighter = {
     new PUTCWeighter(baseFeature, puTopCount, predictFeatures, puLearnerConfig.build())
  }
}

object PUTCWeighterConfig {
  val subclasses = List(classOf[PUTCWeighterConfig]) ++
    PositiveUnlabeledLearnerConfig.subclasses ++
    ProbabilisticClassifierConfig.subclasses
}