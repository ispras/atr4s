package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Based on JATE 2.0:<br>
  *   Zhang, Z., Gao, J., & Ciravegna, F. (2016).
  *   JATE 2.0: Java Automatic Term Extraction with Apache Solr.
  *   The LREC 2016 Proceedings.
  *
  * which, in turn, is based on:<br>
  *   Church, K., & Gale, W. (1999).
  *   Inverse document frequency (idf): A measure of deviations from poisson.
  *   In Natural language processing using very large corpora (pp. 283-295). Springer Netherlands.
  *
  */
case class ResidualIDF() extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    new ResidualIDFFC(dataset.docsMap.size)
  }
}


class ResidualIDFFC(totalDocsCount: Int) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val docFreq: Int = tc.occurrences.map(_.docName).distinct.size
    val idf: Double = Math.log(totalDocsCount.toDouble / docFreq) / Math.log(2)

    val termFreq: Int = tc.occurrences.size
    val avgTermFreq: Double = termFreq.toDouble / totalDocsCount
    val poisson: Double = Math.exp(-avgTermFreq)
    val expectedIDF: Double = - (Math.log(1 - poisson) / Math.log(2))

    val rIDF: Double = idf - expectedIDF
    rIDF
  }
}