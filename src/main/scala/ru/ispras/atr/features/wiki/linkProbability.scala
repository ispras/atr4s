package ru.ispras.atr.features.wiki

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

import scala.io.Source

/**
  * Normalized frequency of being hyperlink in Wikipedia pages.
  *
  * <br>Based on:
  *   Astrakhantsev, N.: Automatic term acquisition from domain-specific text collection by
  *   using wikipedia. Proceedings of the Institute for System Programming 26(4), 7-20. (2014)
  *
  * @param threshold needed to filter out too small values, because they are usually occur due to markup errors
  * @param fileName containing titles of Wikipedia articles
  *                 (normalized by [[ru.ispras.atr.features.wiki.LegacyPhraseNormalizer]])
  */
case class LinkProbability(threshold: Double = 0.018,
                           fileName: String = "./data/info-measure.txt") extends FeatureConfig {
  //TODO recompute info-measure for newer Wikipedia and better lemmatizer
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val term2LinkProb = Source.fromFile(fileName).getLines().map(line => {
      val termInfoMeasure = line.split('\t')
      termInfoMeasure(2) -> termInfoMeasure(0).toDouble / termInfoMeasure(1).toDouble
    }).toMap
    new LinkProbabilityFC(threshold, term2LinkProb)
  }
}

object LinkProbability {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = LinkProbability()
}

class LinkProbabilityFC(threshold: Double,
                        term2LinkProb: Map[String, Double]) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val initLinkProbability: Double = term2LinkProb.getOrElse(LegacyPhraseNormalizer(tc.lemmas), 0)
    if (initLinkProbability > threshold) {
      initLinkProbability
    }
    else
      0
  }
}

