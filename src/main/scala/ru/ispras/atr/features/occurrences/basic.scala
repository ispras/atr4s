package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  *
  * Basic is a modification of C-Value for intermediate level (of specificity) term extraction.
  * Like original [[ru.ispras.atr.features.occurrences.CValue C-Value]], it can extract multi-word terms only;
  * however, unlike C-Value, Basic promotes term candidates that are part of other term candidates,
  * because such terms are usually served for creation of more specific terms.
  *
  * Based on:
  *   Bordea, G., Buitelaar, P., Polajnar, T.: Domain-independent term extraction through domain modelling.
  *   In: the 10th International Conference on Terminology and Artificial Intelligence (TIA 2013)(2013)<br>
  */
case class Basic(longerTermsCoeff: Double = 0.72,// from Thesis which was published later than the paper
                 //3.5		//from the paper
                 minSubTermSize: Int = 2  //when counting longer terms for candidates, they should have 2 and more words
                ) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    log.trace("Computing candidates to subTerms map")
    val shorter2longerTerms = SubTermsComputer.computeShorter2longerTerms(candidates, minSubTermSize)
    log.trace("Converting maps to needed counts")
    val longerTermsCount = shorter2longerTerms.mapValues(_.size)
    new BasicFC(longerTermsCount, longerTermsCoeff)
  }
}

object Basic {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = Basic()
}

class BasicFC(longerTermsCountMap: Map[Seq[String], Int],
              longerTermsCoeff: Double //alpha
             ) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val lengthInWords = tc.occurrences.head.lemmas.size //t
    val freq = tc.freq //f(t)
    val longerTermsCount = longerTermsCountMap.getOrElse(tc.lemmas, 0) //e_t
    lengthInWords * Math.log(freq) + longerTermsCoeff * longerTermsCount
  }
}

