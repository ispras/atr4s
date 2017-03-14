package ru.ispras.atr.candidates
import scala.util.matching.Regex

/**
  * Filters out or keeps term occurrence depending on its PoS tags.
  */
trait POSPatternChecker {
  def satisfy(nGram: Seq[String]): Boolean
}

/**
  * Filters out term occurrence if its PoS tags do not match the pattern encoded as the regular expression.
  *
  * @param posPattern compiled regexp pattern (should correspond to TreeBank PoS tags)
  */
class RegexPOSPatternChecker(posPattern: Regex) extends POSPatternChecker {
  override def satisfy(nGram: Seq[String]): Boolean = {
    val candidateStr = nGram.mkString("_")
    posPattern.unapplySeq(candidateStr).isDefined
  }
}

trait POSPatternCheckerConfig {
  def build(): POSPatternChecker
}

/**
  * Configuration/builder for RegexPOSPatternChecker
  *
  * By default, we apply the commonly-used pattern extended by allowing prepositions between nouns.
  * <br>Paul Buitelaar and Thomas Eigner. 2009.
  *   Expertise mining from scientific literature.
  *   In Proceedings of the fifth international conference on Knowledge capture (K-CAP '09).
  *   ACM, New York, NY, USA, 171-172. DOI=http://dx.doi.org/10.1145/1597735.1597767
  *
  * @param patternStr regexp string (should correspond to TreeBank PoS tags)
  */
case class RegexPOSPatternCheckerConfig(
//                                      patternStr: String = "(NN(S)?_|JJ_)*(NN(S)?)"
                                        patternStr: String = "(NN(S)?_|JJ_|NNP_|NN(S?)_IN_)*(NN(S)?)") extends POSPatternCheckerConfig {
  override def build(): POSPatternChecker = {
    new RegexPOSPatternChecker(patternStr.r)
  }
}

object RegexPOSPatternCheckerConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = RegexPOSPatternCheckerConfig()
}

object POSPatternCheckerConfig {
  val subclasses = List(classOf[RegexPOSPatternCheckerConfig])
}