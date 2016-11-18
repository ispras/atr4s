package ru.ispras.atr.features.keyrel

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.candidates.{TermOccurrencesCollector, TermOccurrencesCollectorConfig}
import ru.ispras.atr.datamodel.{DSDataset, DSDocument, TermOccurrence}
import ru.ispras.atr.features.occurrences.SubTermsComputer

import scala.collection.immutable.IndexedSeq

/**
  * Extract key concepts for the whole document collection:
  * <li>1. extract topCountPerDoc key concepts from each document;
  * <li>2. keep totalKeys key concepts with maximal frequency (number of being chosen as a key concept).
  *
  * @param docKeysExtractorConfig specifies how to extract keys from 1 document
  * @param totalKeysCount         number of keys (concepts or phrases) to extract
  * @param topCountPerDoc         number of keys (concepts or phrases) to be extracted from one document
  * @param minKeyTotalFreq        minimum frequency of key (concept or phrase) to be considered as a key candidate
  * @param keepOnlyConcepts       boolean indicating if we keep only concepts, or extract phrases
  * @param word2VecAdapter        specifies word2vec adapter used for similarity computing
  */
class KeyVectorsExtractor(docKeysExtractorConfig: DocKeysExtractorConfig,
                          totalKeysCount: Int,
                          topCountPerDoc: Int,
                          minKeyTotalFreq: Int,
                          keepOnlyConcepts: Boolean,
                          word2VecAdapter: Word2VecAdapter) {
  val log = LogManager.getLogger(getClass)

  def extract(dataset: DSDataset): Seq[Seq[Array[Float]]] = {
    val docKeysExtractor = docKeysExtractorConfig.build(keepOnlyConcepts, word2VecAdapter)
    val keyCandidatses = dataset.docsMap.values.par.flatMap(doc =>
      docKeysExtractor.extract(doc, topCountPerDoc)).toSeq
    val keysWithWeight = keyCandidatses.groupBy(identity).mapValues(_.size)
      .filter(_._2 >= minKeyTotalFreq)
      .seq.toSeq.sortBy(-_._2)

    log.debug("==best keys==")
    keysWithWeight.take(totalKeysCount).foreach(log.debug(_))

    val keys = keysWithWeight.map(_._1).par
    val keyVectors =
      if (keepOnlyConcepts) {
        keys.take(totalKeysCount).map(word2VecAdapter.getConceptVector)
      } else {
        keys.take(totalKeysCount*2) //we take more than totalKeys for the case of words without corresponding vector,
          // but we do not take all keys in order to not go to word2vec model for each of these phrase
        .map(word2VecAdapter.getVectors).filter(_.nonEmpty).take(totalKeysCount)
      }
    keyVectors.seq
  }
}

/**
  * Extracts keys (concepts or any phrases) from one document.
  * Candidates to keys are only those phrases that
  * occur in the first part of doc and occur more frequently than threshold.
  *
  * @param termOccCollector specifies candidates to be extracted
  * @param leastSeenFreq    minimal frequency of phrase (in the whole text) to be considered as a key candidate
  * @param firstWordsToTake count of first words that can be candidates
  * @param maxNGramSize     maximum length of key phrase
  * @param weighterConfig   specifies how to weigh candidate
  * @param keepOnlyConcepts boolean indicating if we keep only concepts, or extract phrases
  * @param word2VecAdapter  specifies word2vec adapter used for similarity computing
  */
class DocKeysExtractor(termOccCollector: TermOccurrencesCollector,
                       leastSeenFreq: Int,
                       firstWordsToTake: Int,
                       maxNGramSize: Int,
                       weighterConfig: KeyPhraseWeighterConfig,
                       keepOnlyConcepts: Boolean,
                       word2VecAdapter: Word2VecAdapter) {
  val log = LogManager.getLogger(getClass)
  def extract(doc: DSDocument, topCount: Int): Iterable[Seq[String]] = {
    //collect candidates
    val allCandidates: IndexedSeq[TermOccurrence] = (1 to maxNGramSize).flatMap(nGramSize =>
      termOccCollector.collect(doc, nGramSize))
    val cand2freq: Map[Seq[String], Int] = allCandidates.groupBy(_.lemmas).mapValues(_.size)
    val keyCandidates = allCandidates
        .filter(_.endWordIndex < firstWordsToTake)
        .map(_.lemmas)
        .filter(cand2freq(_) >= leastSeenFreq)
        .distinct

    val finalCandidates =
      if (keepOnlyConcepts) {
        keyCandidates.filter(word2VecAdapter.containsConcept)
      } else {
        keyCandidates
      }

    val weighter = weighterConfig.build(allCandidates, cand2freq)

    val bestCandidates = finalCandidates.map(cand => (cand, weighter(cand))).sortBy(-_._2).take(topCount)//.map(_._1)
    log.trace("==next doc==")
    bestCandidates.foreach(log.trace(_))
    bestCandidates.map(_._1)
  }
}

/**
  * Configuration/Builder for [[ru.ispras.atr.features.keyrel.DocKeysExtractor]]
  */
case class DocKeysExtractorConfig(termOccCollectorConfig: TermOccurrencesCollectorConfig = TermOccurrencesCollectorConfig(),
                                  leastSeenFreq: Int = 2,
                                  firstWordsToTake: Int = 800,
                                  maxNGramSize: Int = 3,
                                  weighterConfig: KeyPhraseWeighterConfig = FPLKeyPhraseWeighterConfig()) {
  def build(keepOnlyConcepts: Boolean, word2VecAdapter: Word2VecAdapter) = {
    new DocKeysExtractor(termOccCollectorConfig.build(), leastSeenFreq, firstWordsToTake, maxNGramSize, weighterConfig,
      keepOnlyConcepts, word2VecAdapter)
  }
}

//=== weighters ==//

/**
  * Weights key candidate
  */
trait KeyPhraseWeighter {
  def apply(words: Seq[String]): Double
}

/**
  * Weights key candidate by RAKE algorithm:
  * <br> Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010).
  * Automatic keyword extraction from individual documents. Text Mining, 1-20.
  */
class RakeKeyPhraseWeighter(cand2freq: Map[Seq[String], Int],
                            longerTermsSumFreq: Map[Seq[String], Int]) extends KeyPhraseWeighter {
  override def apply(words: Seq[String]): Double = {
    val itselfFreq = cand2freq(words)
    (itselfFreq + longerTermsSumFreq.getOrElse(words, 0)).asInstanceOf[Double] / itselfFreq
  }
}

/**
  * Weights key candidate by adaptation of KPMiner algorithm:
  * simply product of length (in words) and frequency (in the whole document);
  */
class FPLKeyPhraseWeighter(cand2freq: Map[Seq[String], Int]) extends KeyPhraseWeighter {
  override def apply(words: Seq[String]): Double = {
    words.size * cand2freq(words)
  }
}

/**
  * Configuration/Builder for [[ru.ispras.atr.features.keyrel.KeyPhraseWeighter]]
  */
trait KeyPhraseWeighterConfig {
  def build(allCandidates: IndexedSeq[TermOccurrence], cand2freq: Map[Seq[String], Int]): KeyPhraseWeighter
}

case class RakeKeyPhraseWeighterConfig() extends KeyPhraseWeighterConfig {
  override def build(allCandidates: IndexedSeq[TermOccurrence], cand2freq: Map[Seq[String], Int]): KeyPhraseWeighter = {
    val shorter2longerTerms = SubTermsComputer.computeShorter2longerCollocations(allCandidates.map(_.lemmas), minSubTermSize = 1)
    val longerTermsSumFreq = shorter2longerTerms.mapValues(_.map(subTerm => cand2freq(subTerm)).sum)
    new RakeKeyPhraseWeighter(cand2freq, longerTermsSumFreq)
  }
}

case class FPLKeyPhraseWeighterConfig() extends KeyPhraseWeighterConfig {
  override def build(allCandidates: IndexedSeq[TermOccurrence], cand2freq: Map[Seq[String], Int]): KeyPhraseWeighter = {
    new FPLKeyPhraseWeighter(cand2freq)
  }
}