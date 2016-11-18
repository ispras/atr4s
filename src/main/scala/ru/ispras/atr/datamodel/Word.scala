package ru.ispras.atr.datamodel

/**
  * Word enriched by preprocessing input text documents:
  *
  * @param lemma  lemma (base form) of the word
  * @param posTag part-of-speech tag of the word
  */
case class Word(lemma: String,
                posTag: String)
