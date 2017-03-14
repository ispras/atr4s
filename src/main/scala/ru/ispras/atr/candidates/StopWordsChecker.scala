package ru.ispras.atr.candidates

import scala.io.Source

/**
  * Filters out term occurrence if the predefined set of stop words contains at least one lemma.
  *
  * @param stopWords set of stop words, i.e. words that can't be part of true term.
  */
class StopWordsChecker(stopWords: Set[String]) {
  def satisfy(termLemmas: Iterable[String]): Boolean = {
    stopWords.intersect(termLemmas.map(_.toLowerCase).toSet).isEmpty
  }

  def satisfy(word: String): Boolean = {
    satisfy(Set(word))
  }
}

/**
  * Configuration/builder for StopWordsChecker
  *
  * @param fileName containing stop-words (one word in line).
  *                 By default, we use stop words list from the SMART retrieval system.
  */
case class StopWordsCheckerConfig(fileName: String = "/stopWords.txt") {
  def build() = {
    val stopWords = Source.fromURL(getClass.getResource(fileName))
      .getLines().filter(!_.startsWith("#")).toSet
    new StopWordsChecker(stopWords)
  }
}

object StopWordsCheckerConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = StopWordsCheckerConfig()

  val subclasses = List(classOf[StopWordsCheckerConfig])
}