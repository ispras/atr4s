package ru.ispras.atr.datamodel

/**
  * One text document after preprocessing.
  *
  * @param name  unique id of the document
  * @param words seq of preprocessed words
  */
case class DSDocument(name: String,
                      words: Seq[Word]) {
}
