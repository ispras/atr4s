package ru.ispras.atr.datamodel

/**
  * Encapsulates info about speicifc dataset.
  * Needed for caching purposes mainly.
  */
case class DataConfig(docsDir: String,
                      expectedTermsFilename: String,
                      defaultTopCount: Int,
                      minTermFreq: Int = 2) {
}

object DataConfig {
  val subclasses = List(classOf[DataConfig])
}
