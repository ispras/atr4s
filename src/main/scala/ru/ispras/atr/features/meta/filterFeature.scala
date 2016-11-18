package ru.ispras.atr.features.meta

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Implements filtering logic: if the value of the first feature is 0, then returns 0;
  * otherwise returns the value of the second feature.
  */
case class FilterFeature(filter: FeatureConfig,
                         feature: FeatureConfig) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    new FilterFC(filter.build(candidates, dataset), feature.build(candidates, dataset))
  }
}

class FilterFC(filterFC: FeatureComputer, featureComputer: FeatureComputer) extends FeatureComputer {
  val eps = 1e-6
  override def compute(tc: TermCandidate): Double = {
    val filterVal = filterFC.compute(tc)
    if (filterVal < eps)
      0
    else {
      featureComputer.compute(tc)
    }
  }

  override def compute(candidates: Seq[TermCandidate]): Seq[Double] = {
    val filterVals = filterFC.compute(candidates)
    val featureVals = featureComputer.compute(candidates)
    filterVals.zip(featureVals).map(f => if (f._1 < eps) 0 else f._2)
  }
}

