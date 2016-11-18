package ru.ispras.atr.features.contexts

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.meta.LinearCombinationFeature
import ru.ispras.atr.features.occurrences.Basic
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * Just a linear combinations of [[ru.ispras.atr.features.occurrences.Basic Basic]] and
  * [[ru.ispras.atr.features.contexts.DomainCoherence DomainCoherence]].
  *
  * <br>Based on:
  * Bordea, G., Buitelaar, P., Polajnar, T.: Domain-independent term extraction through domain modelling.<br>
  * In: the 10th International Conference on Terminology and Artificial Intelligence (TIA 2013)(2013)<br>
  *
  * (NB: this is config/builder that only initializes corresponding FeatureComputer.)
  */
case class PostRankDC(basic: Basic = Basic(),
                      domainCoherence: DomainCoherence = DomainCoherence()) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    LinearCombinationFeature(Seq(basic, domainCoherence)).build(candidates, dataset)
  }
}
