package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Modifies [[ru.ispras.atr.features.occurrences.Basic Basic]] further,
  * so that the level of term specificity can be customized by changing parameters of the method.
  *
  * <br>Based on:
  *   Astrakhantsev, N.: Methods and software for terminology extraction from domain-specific text collection.
  *   Ph.D. thesis, Institute for System Programming of Russian Academy of Sciences (2015).<br>
  *
  * @param longerTermsCoeff alpha - by increasing alpha, one can extract more intermediate terms
  * @param shorterTermsCoeff beta - by increasing beta, one can extract more specific terms
  */
case class ComboBasic(longerTermsCoeff: Double, //alpha
                      shorterTermsCoeff: Double //beta
                     ) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    log.trace("Computing candidates to subTerms map")
    //longer terms should be counted for at least 2-worded terms (see Basic description)
    val shorter2longerTerms = SubTermsComputer.computeShorter2longerTerms(candidates, minSubTermSize = 2)
    val longer2shorterTerms = SubTermsComputer.computeLonger2shorterTerms(candidates)

    log.trace("Converting maps to needed counts")
    val shorterTermsCount = longer2shorterTerms.mapValues(_.size)
    val longerTermsCount = shorter2longerTerms.mapValues(_.size)

    new ComboBasicFC(longerTermsCount, shorterTermsCount, longerTermsCoeff, shorterTermsCoeff)
  }
}

class ComboBasicFC(longerTermsCountMap: Map[Seq[String], Int],
                   shorterTermsCountMap: Map[Seq[String], Int],
                   longerTermsCoeff: Double, //alpha
                   shorterTermsCoeff: Double //betta
                  ) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val lengthInWords = tc.lengthInWords //t
    val freq = tc.freq //f(t)
    val longerTermsCount = longerTermsCountMap.getOrElse(tc.lemmas, 0) //e_t
    val shorterTermsCount = shorterTermsCountMap.getOrElse(tc.lemmas, 0) //e'_t
    lengthInWords * Math.log(freq) + longerTermsCoeff * longerTermsCount + shorterTermsCoeff * shorterTermsCount
  }
}

