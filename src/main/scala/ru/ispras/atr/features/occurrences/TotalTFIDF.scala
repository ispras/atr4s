package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Based on JATE 2.0:<br>
  *   Zhang, Z., Gao, J., & Ciravegna, F. (2016).
  *   JATE 2.0: Java Automatic Term Extraction with Apache Solr.
  *   The LREC 2016 Proceedings.
  */
class TotalTFIDF extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    new TotalTFIDFFC(dataset.sizeInDocs)
  }
}

class TotalTFIDFFC(docsCount: Int) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val docFreq = tc.occurrences.map(_.docName).distinct.size
    val termFreq = tc.occurrences.size
    val tfIdf = termFreq.toDouble * Math.log(docsCount / docFreq.toDouble)
    tfIdf
  }
}