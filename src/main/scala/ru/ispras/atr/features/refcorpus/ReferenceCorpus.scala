package ru.ispras.atr.features.refcorpus

import ru.ispras.atr.features.wiki.LegacyPhraseNormalizer

import scala.io.Source

class ReferenceCorpus(term2Count: Map[String, Int], corpusSize: Int, epsilon: Double) {

  def frequency(tcWords: Seq[String]): Double = {
    termCount(tcWords).toDouble / corpusSize + epsilon
  }

  def termCount(tcWords: Seq[String]): Int = {
    term2Count.getOrElse(LegacyPhraseNormalizer(tcWords), 0)
  }

  def size() = corpusSize
}

/**
  * Based on n-grams frequency computed over the Corpus of Historical American English (COHA).
  * See http://www.ngrams.info/download_coha.asp
  *
  * @param fileName path to file containing normalized term representation and its frequency.
  */
//TODO find better n-grams stats from general domain (e.g. Wikipedia)
case class ReferenceCorpusConfig(fileName: String = "./data/COHA_term_occurrences.txt",
                                 epsilon: Double = 1e-20) {
  def build() = {
    val fileLines = Source.fromFile(fileName).getLines()
    val corpusSize = fileLines.next().toInt
    val term2RefFreq = fileLines.map(line => {
      val termRefFreq = line.split(' ')
      termRefFreq(0) -> termRefFreq(1).toInt  //0 - term itself, 1 - count of its occurrences in the reference corpus
    }).toMap
    new ReferenceCorpus(term2RefFreq, corpusSize, epsilon)
  }
}

object ReferenceCorpusConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = ReferenceCorpusConfig()
}
