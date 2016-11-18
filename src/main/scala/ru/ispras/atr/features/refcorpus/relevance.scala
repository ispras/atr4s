package ru.ispras.atr.features.refcorpus

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Taken from:<br>
  *   Peñas, A., Verdejo, F., Gonzalo, J. (2001, March).
  *   Corpus-based terminology extraction applied to information access.
  *   In Proceedings of Corpus Linguistics (Vol. 2001).<br>
  */
case class Relevance(referenceCorpusConfig: ReferenceCorpusConfig = ReferenceCorpusConfig()) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val targetCorpusSize = dataset.sizeInWords
    val targetCorpusDocsCount = dataset.sizeInDocs
    new RelevanceFC(referenceCorpusConfig.build(), targetCorpusSize, targetCorpusDocsCount)
  }
}

class RelevanceFC(referenceCorpus: ReferenceCorpus,
                  targetCorpusSize: Long,
                  targetCorpusDocsCount: Int) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val termRefFreq = referenceCorpus.frequency(tc.lemmas)
    val termTargetFreq: Double = tc.occurrences.size.toDouble / targetCorpusSize
    val termDocTargetFreq = tc.occurrences.map(_.docName).distinct.size.toDouble / targetCorpusDocsCount

    val relevance = 1 - Math.log(2) / Math.log(2 + termTargetFreq * termDocTargetFreq / termRefFreq)
    relevance
  }
}
