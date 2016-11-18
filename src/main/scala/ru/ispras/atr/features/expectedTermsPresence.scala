package ru.ispras.atr.features

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.preprocess.{EmoryNLPPreprocessorConfig, NLPPreprocessorConfig}
import ru.ispras.atr.utils.ExpectedTermsReader


/**
  * Returns 1 if candidate occurs in expected terms list; 0 otherwise.
  *
  * Can be used as a seed feature for PU-ATR, thus transforming it into semi-supervised learning.
  *
  * @param fileName  name of the file containing expected terms
  * @param nlpConfig lemmatizes expected terms read from the file
  */
case class ExpectedTermsPresence(fileName: String,
                                 nlpConfig: NLPPreprocessorConfig = EmoryNLPPreprocessorConfig()) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val termSet = ExpectedTermsReader(fileName, nlpConfig.build())
    new ExpectedTermsPresenceFC(termSet)
  }
}

class ExpectedTermsPresenceFC(termSet: Set[String]) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val res: Double = if (termSet.contains(tc.canonicalRepr)) 1 else 0
    res
  }
}
