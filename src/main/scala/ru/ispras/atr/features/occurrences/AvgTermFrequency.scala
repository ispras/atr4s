package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Based on JATE 2.0:<br>
  *   Zhang, Z., Gao, J., & Ciravegna, F. (2016).
  *   JATE 2.0: Java Automatic Term Extraction with Apache Solr.
  *   The LREC 2016 Proceedings.
  */
case class AvgTermFrequency() extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = new AvgTermFrequencyFC
}

class AvgTermFrequencyFC extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val docFreq = tc.occurrences.map(_.docName).distinct.size
    val termFreq = tc.occurrences.size
    val avgTF = termFreq.toDouble / docFreq
    avgTF
  }
}