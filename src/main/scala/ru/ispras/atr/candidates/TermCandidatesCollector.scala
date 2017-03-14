package ru.ispras.atr.candidates

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.datamodel.{DSDataset, DSDocument, TermCandidate, TermOccurrence}
import scala.collection.JavaConversions.asScalaBuffer

/**
  * Extracts consecutive word n-grams of specified orders as term occurrences,
  * then combines occurrences with the same canonical representation (lemmas joined by underscore symbol)
  * as belonging to the same term candidate.
  * Finally, filters out term candidates occurring rarer than specified times.
  *
  * @param nGramSizes               sizes of n-grams to be considered (e.g. 1 to 4)
  * @param minTermFreq              minimal frequency of term candidate to be kept
  * @param termOccurrencesCollector collects term occurrence for specified n-gram order
  */
class TermCandidatesCollector(nGramSizes: Seq[Int],
                              minTermFreq: Int,
                              termOccurrencesCollector: TermOccurrencesCollector) {
  val log = LogManager.getLogger(getClass)

  def collect(dataset: DSDataset) : Seq[TermCandidate] = {
    log.info(s"Starting term candidates collection")
    val result = nGramSizes.flatMap(n => collectNgrams(dataset, n))
    log.info(s"Total term candidates: ${result.size}")
    result
  }

  def collectNgrams(dataset: DSDataset, nGramSize: Int) : Seq[TermCandidate] = {
    log.info(s"Starting collection of $nGramSize-grams...")
    val allTermOccurrences = dataset.docsMap.values.par.flatMap(doc => termOccurrencesCollector.collect(doc, nGramSize))
    log.info(s"$nGramSize-grams occurrences: ${allTermOccurrences.size}")
    //create term candidate by grouping term occurrences by canonical representation
    val allTermCandidates = allTermOccurrences.groupBy(TermOccurrence.canonicalRepresentation).toSeq
      .map(tc => TermCandidate(tc._2.toSeq.seq))
    log.info(s"$nGramSize-grams candidates: ${allTermCandidates.size}")
    val frequentTermCandidates = allTermCandidates.filter(_.occurrences.size >= minTermFreq)
    log.info(s"$nGramSize-grams (freq >=$minTermFreq): ${frequentTermCandidates.size}")
    frequentTermCandidates.seq
  }
}

/**
  * Extracts consecutive word n-grams as term occurrences.
  * Three basic filters are applied before term occurrence formation;
  * these filters correspond to class parameters.
  *
  * @param noiseWordsChecker filters out term occurrence based on character properties of its lemmas
  * @param stopWordsChecker  filters out term occurrence based on the predefined set of stop words
  * @param posPatternChecker filters out or keeps term occurrence depending on its PoS tags
  */
class TermOccurrencesCollector(noiseWordsChecker: NoiseWordsChecker,
                               stopWordsChecker: StopWordsChecker,
                               posPatternChecker: POSPatternChecker) {
  def collect(doc: DSDocument, nGramSize: Int): Iterator[TermOccurrence] = {
    //get all words with global index in doc and slide by ngrams
    doc.words.zipWithIndex.sliding(nGramSize)
      //get rid of ngrams with bad chars
      .filter(nGram => noiseWordsChecker.satisfy(nGram.map(_._1.lemma)))
      //get rid of ngrams with stop words
      .filter(nGram => stopWordsChecker.satisfy(nGram.map(_._1.lemma)))
      //get rid of ngrams with bad PoS patterns
      .filter(nGram => posPatternChecker.satisfy(nGram.map(_._1.posTag)))
      //form term occurrence
      .map(nGram => TermOccurrence(nGram.map(_._1.lemma), doc.name, nGram.head._2))
  }
}

/**
  * Configuration/builder for TermOccurrencesCollector
  */
case class TermOccurrencesCollectorConfig(posPatternCheckerConfig: POSPatternCheckerConfig = RegexPOSPatternCheckerConfig(),
                                          stopWordsCheckerConfig: StopWordsCheckerConfig = StopWordsCheckerConfig(),
                                          noiseWordsCheckerConfig: NoiseWordsCheckerConfig = NoiseWordsCheckerConfig()) {
  def build(): TermOccurrencesCollector = {
    new TermOccurrencesCollector(noiseWordsCheckerConfig.build(),
      stopWordsCheckerConfig.build(),
      posPatternCheckerConfig.build())
  }
}

object TermOccurrencesCollectorConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = TermOccurrencesCollectorConfig()

  val subclasses = List(classOf[TermOccurrencesCollectorConfig])
}

/**
  * Configuration/builder for TermCandidatesCollector
  */
trait TermCandidatesCollectorConfig {
  def build(): TermCandidatesCollector
}

case class TCCConfig(nGramSizes: Seq[Int] = 1 to 4,
                     minTermFreq: Int = 2,
                     termOccurrencesCollectorConfig: TermOccurrencesCollectorConfig = TermOccurrencesCollectorConfig())
    extends TermCandidatesCollectorConfig {
  override def build(): TermCandidatesCollector = {
    new TermCandidatesCollector(
      nGramSizes,
      minTermFreq,
      termOccurrencesCollectorConfig.build()
    )
  }
}

object TCCConfig {
  /** constructors for Java, since it doesn't support parameters with default values and can't work with scala seqs */
  def make() = TCCConfig()
  def make(nGramSizes: java.util.List[Int], minTermFreq: Int) = TCCConfig(nGramSizes, minTermFreq)
  def make(nGramSizes: java.util.List[Int], minTermFreq: Int, termOccurrencesCollectorConfig: TermOccurrencesCollectorConfig) =
    TCCConfig(nGramSizes, minTermFreq, termOccurrencesCollectorConfig)
}

object TermCandidatesCollectorConfig {
  val subclasses = List(classOf[TCCConfig]) ++
    CachingTCCConfig.subclasses ++
    TermOccurrencesCollectorConfig.subclasses ++
    NamesOnlyTCCConfig.subclasses ++
    POSPatternCheckerConfig.subclasses ++
    StopWordsCheckerConfig.subclasses ++
    NoiseWordsCheckerConfig.subclasses
}