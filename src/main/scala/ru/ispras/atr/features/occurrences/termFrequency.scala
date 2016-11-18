package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}


/**
  * Simply returns count of term candidate occurrences.
  */
case class TermFrequency() extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = new TermFrequencyFC
}

class TermFrequencyFC extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    tc.occurrences.size
  }
}

