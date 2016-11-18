package ru.ispras.atr.features

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}

/**
  * Computes length of term candidate in words.
  * May be useful for supervised algorithms.
  */
case class WordsCount() extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = new WordsCountFC
}

class WordsCountFC extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    tc.lengthInWords
  }
}