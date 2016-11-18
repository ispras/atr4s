package ru.ispras.atr.features.wiki

/**
  * This normalization was used in previous version of the tool,
  * so we have to repeat it in order to reuse artifacts from that tool.
  */
object LegacyPhraseNormalizer {
  def apply(phrase: Seq[String]): String = {
    phrase.map(normalizeWord(_)).mkString
  }

  def normalizeWord(word: String): String = {
    val uppercaseWord = word.toUpperCase
    val len = word.length
    val result: String =
      if (len < 3) {
        uppercaseWord
      } else if (uppercaseWord.endsWith("SES") || uppercaseWord.endsWith("ZES")
        || uppercaseWord.endsWith("SHES") || uppercaseWord.endsWith("CHES")
        || uppercaseWord.endsWith("XES")) {
        uppercaseWord.substring(0, len - 2)
      } else if (uppercaseWord.endsWith("IES")) {
        uppercaseWord.substring(0, len - 3) + "Y"
      } else if (uppercaseWord.endsWith("VES")) {
        uppercaseWord.substring(0, len - 3) + "F"
      } else if (uppercaseWord.endsWith("S") && !uppercaseWord.endsWith("SS")) {
        uppercaseWord.substring(0, len - 1)
      } else {
        uppercaseWord
      }
    result
  }
}
