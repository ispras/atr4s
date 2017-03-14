package ru.ispras.atr.features.refcorpus

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Taken from:<br>
  *   Ahmad, K., Gillam, L., & Tostevin, L. (1999).
  *   University of Surrey Participation in TREC8:
  *   Weirdness Indexing for Logical Document Extrapolation and Retrieval (WILDER).
  *   In TREC.<br>
  */
case class Weirdness(referenceCorpusConfig: ReferenceCorpusConfig = ReferenceCorpusConfig(epsilon = 0.001)) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val refCorpus = referenceCorpusConfig.build()
    val targetCorpusSize = dataset.sizeInWords
    new WeirdnessFC(refCorpus, targetCorpusSize)
  }
}

object Weirdness {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = Weirdness()
}

class WeirdnessFC(referenceCorpus: ReferenceCorpus,
                  targetCorpusSize: Long) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val termRefFreq: Double = referenceCorpus.frequency(tc.lemmas)
    val termTargetFreq = tc.occurrences.size.toDouble / targetCorpusSize
    val weirdness = termTargetFreq / termRefFreq
    weirdness
  }
}