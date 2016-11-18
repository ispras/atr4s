package ru.ispras.atr.rank

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.datamodel.{DSDataset, Identifiable, TermCandidate}
import ru.ispras.atr.features.FeatureConfig

/**
  * Since term candidates ranking becomes non-trivial in case of multiple methods for term candidates scoring,
  * General idea for this problem is to aggregate values of multiple features into one number, usually between 0 and 1,
  * thus reducing the task to ranking by one method.
  *
  * Implementations of this class are supposed to compute feature values for term candidates (i.e. term scoring);
  * perform such an aggregation;
  * and rank by obtained final weight.
  */
trait TermCandidatesWeighter extends Identifiable {
  val log = LogManager.getLogger(getClass)

  def weightAndSort(candidates: Seq[TermCandidate], dataset: DSDataset): Iterable[(String, Double)]
}

trait TermCandidatesWeighterConfig {
  def build(): TermCandidatesWeighter
}

object TermCandidatesWeighterConfig {
  val subclasses = List(
    classOf[OneFeatureTCWeighterConfig],
    classOf[SparkOneFeatureTCWeighterConfig],
    classOf[VotingTCWeighterConfig]) ++
    PUTCWeighterConfig.subclasses ++
    FeatureConfig.subclasses
}