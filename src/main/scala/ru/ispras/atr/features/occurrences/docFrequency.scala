package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Simply returns count of document, where candidate has at least one occurrence.
  */
case class DocFrequency() extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = new DocFrequencyFC
}


class DocFrequencyFC extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    tc.occurrences.map(_.docName).distinct.size
  }
}

