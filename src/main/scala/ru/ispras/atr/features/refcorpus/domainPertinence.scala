package ru.ispras.atr.features.refcorpus

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Taken from:<br>
  *   Meijer, K., Frasincar, F., & Hogenboom, F. (2014).
  *   A semantic approach for extracting domain taxonomies from text.
  *   Decision Support Systems, 62, 78-93.<br>
  */
case class DomainPertinence(referenceCorpusConfig: ReferenceCorpusConfig = ReferenceCorpusConfig(),
                            notFoundTermSmoothing: Double = 0.1) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    new DomainPertinenceFC(referenceCorpusConfig.build(), notFoundTermSmoothing)
  }
}

class DomainPertinenceFC(referenceCorpus: ReferenceCorpus,
                         notFoundTermSmoothing: Double) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val termRefCount: Double = referenceCorpus.termCount(tc.lemmas) + notFoundTermSmoothing
    val termTargetCount = tc.occurrences.size

    val domainRelevance = termTargetCount / termRefCount
    domainRelevance
  }
}