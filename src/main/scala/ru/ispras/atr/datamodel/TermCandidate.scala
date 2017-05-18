package ru.ispras.atr.datamodel

/**
  * Collection of term occurrences corresponding to one term candidate
  * (it is assumed that all occurrences have the same canonical representation
  */
case class TermCandidate(occurrences: Seq[TermOccurrence]) {
  def freq = occurrences.size

  def lemmas = occurrences.head.lemmas

  def canonicalRepr: String = TermOccurrence.canonicalRepresentation(occurrences.head)

  def verboseRepr(docsToShow: Int): String = {
    canonicalRepr + (if (docsToShow < 1) {
      ""
    } else {
      val docNames: Seq[String] = occurrences.map(_.docName).distinct
      val docNamesStr = docNames.slice(0, docsToShow).mkString(",") + (if (docNames.size > docsToShow) {
        "..."
      } else {
        ""
      })
      s" [$docNamesStr]"
    })
  }

  def lengthInWords: Int = occurrences.head.lemmas.size
}
