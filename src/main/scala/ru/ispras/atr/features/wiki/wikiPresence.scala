package ru.ispras.atr.features.wiki

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

import scala.io.Source

/**
  * Returns 1 if Wikipedia contains article with the same name as the candidate; 0 otherwise.
  *
  * Should be used in conjunction with other features (see [[ru.ispras.atr.features.meta.FilterFeature]]) \
  * as a seed for PU-ATR.
  *
  * @param fileName containing titles of Wikipedia articles
  *                 (normalized by [[ru.ispras.atr.features.wiki.LegacyPhraseNormalizer]])
  */
case class WikiPresenceFeature(fileName: String = "/info-measure.txt") extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val termInWiki = Source.fromURL(getClass.getResource(fileName)).getLines().map(line => {
      val termInfoMeasure = line.split('\t')
      termInfoMeasure(2)
    }).toSet
    new WikiPresenceFC(termInWiki)
  }
}

object WikiPresenceFeature {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = WikiPresenceFeature()
}

class WikiPresenceFC(termInWiki: Set[String]) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val res: Double = if (termInWiki.contains(LegacyPhraseNormalizer(tc.lemmas))) 1 else 0
    res
  }
}
