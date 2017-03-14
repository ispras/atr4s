package ru.ispras.atr.datamodel

/**
  * Encapsulates info about specific dataset.
  * Needed for caching purposes mainly.
  */
case class DataConfig(docsDir: String,
                      expectedTermsFilename: String,
                      defaultTopCount: Int,
                      minTermFreq: Int = 2) {
}

object DataConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make(docsDir: String, expectedTermsFilename: String, defaultTopCount: Int) =
    DataConfig(docsDir, expectedTermsFilename,defaultTopCount)

  val subclasses = List(classOf[DataConfig])
}
