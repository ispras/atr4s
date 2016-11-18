package ru.ispras.atr.features

import ru.ispras.atr.datamodel.TermCandidate


/**
  * Feature computing, or term candidates scoring,
  * i.e. assigning a number to each term candidate reflecting its likelihood of being a term,
  * is the most important and sophisticated step in the whole pipeline.
  */
trait FeatureComputer {

  /**
    * Uses parallel collections to compute feature values for each candidate
    */
  def compute(candidates: Seq[TermCandidate]): Seq[Double] = {
    candidates.par.map(compute(_)).seq
  }

  def compute(tc: TermCandidate): Double
}
