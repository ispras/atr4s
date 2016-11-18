package ru.ispras.atr.features

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.datamodel.{DSDataset, Identifiable, TermCandidate}
import ru.ispras.atr.features.contexts.{DomainCoherence, PostRankDC, PMI}
import ru.ispras.atr.features.keyrel.KeyPhraseRelatedness
import ru.ispras.atr.features.meta.{FilterFeature, LinearCombinationFeature}
import ru.ispras.atr.features.occurrences._
import ru.ispras.atr.features.refcorpus.{DomainPertinence, ReferenceCorpusConfig, Relevance, Weirdness}
import ru.ispras.atr.features.tm.NovelTopicModel
import ru.ispras.atr.features.wiki.{LinkProbability, WikiPresenceFeature}

/**
  * Configuration/builder for FeatureComputer.
  */
trait FeatureConfig extends Identifiable {
  val log = LogManager.getLogger(getClass)

  def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer
}

object FeatureConfig {
  val subclasses = List(
    classOf[FeatureConfig],
    classOf[Basic],
    classOf[ComboBasic],
    classOf[CValue],
    classOf[DocFrequency],
    classOf[DomainCoherence], classOf[PMI],
    classOf[PostRankDC],
    classOf[LinearCombinationFeature],
    classOf[LinkProbability],

    classOf[ReferenceCorpusConfig],
    classOf[Relevance],
    classOf[Weirdness],
    classOf[DomainPertinence],

    classOf[TermFrequency],
    classOf[AvgTermFrequency],
    classOf[TotalTFIDF],
    classOf[ResidualIDF],

    classOf[NovelTopicModel],
    classOf[WordsCount],
    classOf[FilterFeature],
    classOf[WikiPresenceFeature],
    classOf[ExpectedTermsPresence],
    classOf[RandomFeature],
    classOf[CachingFeature]
  ) ++ KeyPhraseRelatedness.subclasses
}