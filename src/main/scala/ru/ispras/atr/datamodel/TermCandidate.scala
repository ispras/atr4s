package ru.ispras.atr.datamodel

/**
  * Collection of term occurrences corresponding to one term candidate
  * (it is assumed that all occurrences have the same canonical representation
  */
case class TermCandidate(occurrences: Seq[TermOccurrence]) {
  def freq = occurrences.size

  def lemmas = occurrences.head.lemmas

  def canonicalRepr = TermOccurrence.canonicalRepresentation(occurrences.head)

  def lengthInWords: Int = occurrences.head.lemmas.size
}
