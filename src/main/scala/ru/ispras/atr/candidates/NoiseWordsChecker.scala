package ru.ispras.atr.candidates
import ru.ispras.atr.datamodel.Word

import scala.util.matching.Regex

/**
  * Filters out term occurrence if at least one lemma
  * has length less than pre-specified number characters or
  * doesn't satisfy pre-specified regular expression (e.g. contains non-alphanumeric characters).
  *
  * It is also can be used for filtering words (based on lemma or/and part-of-speech tag), not only term occurrences.
  *
  * This filter is most useful for texts obtained from automatic parsing (e.g. PDF or HTML)
  * and thus containing a lot of noise words.
  *
  * @param minWordLength        minimal length of lemma in characters
  * @param acceptableCharsRegex regular expression such that the lemma must satisfy
  * @param validPoSTags         set of valid part-of-speech tags (e.g. if need to keep only semantics-bearing words)
  */
class NoiseWordsChecker(minWordLength: Int,
                        acceptableCharsRegex: Regex,
                        validPoSTags: Set[String]) {

  /**
    * @param termLemmas lemmas to be checked (usually: of term occurrence)
    * @return true iff all of its lemmas have length not less than the predefined limit and
    *         match the predefined regular expression
    */
  def satisfy(termLemmas: Seq[String]): Boolean = {
    !termLemmas.exists(l => !isGoodLemma(l))
  }

  def isGoodLemma(lemma: String) = {
    lemma.length >= minWordLength && acceptableCharsRegex.unapplySeq(lemma).isEmpty
  }

  def isGoodPoS(posTag: String) = {
    validPoSTags.contains(posTag)
  }

  def isGoodWord(word: Word) = {
    isGoodLemma(word.lemma) && isGoodPoS(word.posTag)
  }
}

/**
  * Configuration/builder for NoiseWordsChecker
  *
  * @param minWordLength        minimal length of lemma in characters
  * @param acceptableCharsRegex regular expression such that the lemma must satisfy
  * @param validPoSTags         set of valid part-of-speech tags (e.g. if need to keep only semantics-bearing words)
  */
case class NoiseWordsCheckerConfig(validPoSTags: Set[String] = NoiseWordsCheckerConfig.goodWordsPoSes,
                                   minWordLength: Int = 3,
                                   acceptableCharsRegex: String = "[^\\p{L}\\p{N}\\-]+" //letters and digits and dash
//                                   acceptableCharsRegex: String = "[^\\p{L}]+" //only letters
                                  ) {
  def build(): NoiseWordsChecker = {
    new NoiseWordsChecker(minWordLength, acceptableCharsRegex.r.unanchored, validPoSTags)
  }
}

object NoiseWordsCheckerConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = NoiseWordsCheckerConfig()

  val nouns: Set[String] = Set("NN", "NNS", "NNP", "NNPS")
  val verbs: Set[String] = Set("VB", "VBD","VBG", "VBN", "VBP", "VBZ")
  val adjectives: Set[String] = Set("JJ", "JJR", "JJS")
  val adverbs: Set[String] = Set("RB", "RBR", "RBS")

  val semanticBearingPoSes: Set[String] = nouns.union(verbs).union(adjectives)
  val goodWordsPoSes: Set[String] = semanticBearingPoSes.union(adverbs)

  val subclasses = List(classOf[NoiseWordsChecker])
}