package ru.ispras.atr.features.meta

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}
import scala.collection.JavaConversions.asScalaBuffer

/**
  * Combines multiple features in linear combination.
  *
  * @param innerFeatures feature configurations to be used in linear combination
  * @param coefficients to be used in linear combination;
  *                     if None, then all coefficients are set equal (so that arithmetic mean is returned)
  */
case class LinearCombinationFeature(innerFeatures: Seq[FeatureConfig],
                                    coefficients: Option[Seq[Double]] = None) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    new LinearCombinationFC(innerFeatures.map(_.build(candidates, dataset)), coefficients)
  }
}

object LinearCombinationFeature {
  /** constructors for Java, since it doesn't support parameters with default values and can't work with scala seqs */
  def make(innerFeatures: java.util.List[FeatureConfig]) = LinearCombinationFeature(innerFeatures)
  def make(innerFeatures: java.util.List[FeatureConfig], coefficients: java.util.List[Double]) =
    LinearCombinationFeature(innerFeatures, Some(coefficients))
}

class LinearCombinationFC(featureComputers: Seq[FeatureComputer],
                          coefficients: Option[Seq[Double]]) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    coefficients match {
      case None => featureComputers.map(_.compute(tc)).sum / featureComputers.size //equal coefficients
      case Some(coeffs) => featureComputers.zip(coeffs).map(fWithCoeff => fWithCoeff._1.compute(tc) * fWithCoeff._2).sum
    }
  }
}