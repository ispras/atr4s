package ru.ispras.atr.datamodel

/**
  * Basic element corresponding to one occurrence of term candidate.
  *
  * @param lemmas         lemmas constituting term occurrence
  * @param docName        name of the document, where term candidate occurs
  * @param startWordIndex index of the first word of the term occurrence in the document
  */
case class TermOccurrence(lemmas: Seq[String],
                          docName: String,
                          startWordIndex: Int) {
  def endWordIndex = startWordIndex + lemmas.size - 1
}

object TermOccurrence {
  val delim = "_"
  def canonicalRepresentation(occ: TermOccurrence): String = canonicalRepresentation(occ.lemmas)
  def canonicalRepresentation(lemmas: Seq[String]): String = lemmas.mkString(delim)
}