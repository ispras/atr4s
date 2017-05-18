package ru.ispras.atr.rank

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.FeatureConfig
import ru.ispras.atr.features.keyrel.{KeyConceptRelatedness, KeyConceptRelatednessFC}

/**
  * Simply ranks by the specified feature.
  */
class OneFeatureTCWeighter(feature: FeatureConfig, docsToShow: Int) extends TermCandidatesWeighter {

  def weightAndSort(candidates: Seq[TermCandidate], dataset: DSDataset): Iterable[(String, Double)] = {
    log.info(s"Initializing feature ${feature.id}...")
    val featureComputer = feature.build(candidates, dataset)
    log.info(s"Computing feature ${feature.id}...")
    val featureVals: Seq[Double] = featureComputer.compute(candidates)
    //hack for computing number of candidates occurring in Wikipedia as concepts
//    val keyRel = featureComputer.asInstanceOf[KeyConceptRelatednessFC]
//    log.debug(s"hits: ${keyRel.word2VecAdapter.hits}; misses: ${keyRel.word2VecAdapter.misses}")
    val res: Seq[(String, Double)] = candidates.map(_.verboseRepr(docsToShow)).zip(featureVals).sortBy(-_._2)
    res
  }
}

case class OneFeatureTCWeighterConfig(feature: FeatureConfig, docsToShow: Int = 3) extends TermCandidatesWeighterConfig {
  override def build(): TermCandidatesWeighter = new OneFeatureTCWeighter(feature, docsToShow)
}
