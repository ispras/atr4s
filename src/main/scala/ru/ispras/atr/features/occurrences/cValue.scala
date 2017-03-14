package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel._
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * One of the most popular method.
  * Promotes term candidates that occur frequently, but not as parts of other term candidates.
  * Modification proposed by Ventura for 1-word terms:
  * originally, CValue was supposed to work with multi-word term candidates only.
  *
  * <br>Based on:
  *   Frantzi, K., Ananiadou, S., Mima, H.: Automatic recognition of multi-word terms:.
  *   the c-value/nc-value method. International Journal on Digital Libraries 3(2), 115-130 (2000).
  *
  * <br> Modification based on:
  *   Ventura, J.A.L., Jonquet, C., Roche, M., Teisseire, M., et al.: Combining c-value and
  *   keyword extraction methods for biomedical terms extraction. In: LBM'2013: International
  *   Symposium on Languages in Biology and Medicine, pp. 45{49 (2013)
  *
  * @param smoothing applied for processing 1-word term candidates
  */
case class CValue(smoothing: Double = 0.1) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    log.trace("Computing candidates to subTerms map")
    val shorter2longerTerms = SubTermsComputer.computeShorter2longerTerms(candidates, minSubTermSize = 1)

    log.trace("Converting maps to needed counts")
    val longerTermsCount = shorter2longerTerms.mapValues(_.size)

    val existedTermCandidate2Freq = candidates.map(c => (c.lemmas, c.occurrences.size)).toMap
    val longerTermsSumFreq = shorter2longerTerms
      .mapValues(_.map(subTerm => existedTermCandidate2Freq(subTerm)).sum)

    new CValueFC(longerTermsCount, longerTermsSumFreq, smoothing)
  }
}

object CValue {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = CValue()
}

class CValueFC(longerTermsCountMap: Map[Seq[String], Int],
               longerTermsSumFreqMap: Map[Seq[String], Int],
               smoothing: Double) extends FeatureComputer{

  override def compute(tc: TermCandidate): Double = {
    val basis = Math.log(tc.lengthInWords + smoothing) / Math.log(2)
    val longerTermsCount = longerTermsCountMap.getOrElse(tc.lemmas, 0)
    val reduce = if (longerTermsCount > 0) {
      longerTermsSumFreqMap(tc.lemmas) / longerTermsCount.asInstanceOf[Double]
    } else {
      0
    }
    basis * (tc.freq - reduce)
  }
}