package ru.ispras.atr.datamodel

/**
  * Collection of preprocessed documents
  * (map is needed for fast document retrieval by term occurrence field,
  * which, in turn, is needed for some features, e.g. DomainCoherence)
  */
case class DSDataset(docsMap: Map[String, DSDocument]) {

  lazy val sizeInWords: Long = docsMap.values.map(_.words.size.toLong).sum
  lazy val sizeInDocs: Int = docsMap.size
}
