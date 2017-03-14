package ru.ispras.atr.preprocess

import ru.ispras.atr.datamodel.{DSDataset, DSDocument, DataConfig, Word}
import ru.ispras.atr.utils.Cacher


/**
  * Decorator for caching results of Preprocessor
  *
  * @param innerPreprocessor actual Preprocessor, which is used for reading (tokenizing and lemmatizing) expected terms list
  * @param cacher helper that works with cache
  */
class CachingNLPPreprocessor(innerPreprocessor: NLPPreprocessor,
                             cacher: Cacher[CachingNLPPreprocessorConfig, DSDataset, String]) extends NLPPreprocessor {
  override def preprocess(dirName: String) : DSDataset = {
    val dataset = cacher.getFromCache(dirName)
    log.debug(s"Dataset ($dirName) size in docs: ${dataset.sizeInDocs}; in words: ${dataset.sizeInWords}")
    dataset
  }

  override def extractWords(s: String) = innerPreprocessor.extractWords(s)
}

/**
  * Configuration/builder for [[ru.ispras.atr.preprocess.CachingNLPPreprocessor]]
  *
  * @param dataConfig         configuration of data to be preprocessed
  * @param preprocessorConfig configuration of preprocessor, which should be cached
  * @param cacheDirName       name of subdirectory, where this cache will be stored
  */
case class CachingNLPPreprocessorConfig(dataConfig: DataConfig,
                                        preprocessorConfig: NLPPreprocessorConfig,
                                        cacheDirName: String = "datasets/"
                                   ) extends NLPPreprocessorConfig {
  override def build(): NLPPreprocessor = {
    val innerEnricher: NLPPreprocessor = preprocessorConfig.build()

    def recreateObject(originalParams: String): DSDataset = innerEnricher.preprocess(originalParams)

    new CachingNLPPreprocessor(innerEnricher,
      new Cacher[CachingNLPPreprocessorConfig, DSDataset, String](this, cacheDirName, recreateObject))
  }
}

object CachingNLPPreprocessorConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make(dataConfig: DataConfig, preprocessorConfig: NLPPreprocessorConfig) =
    CachingNLPPreprocessorConfig(dataConfig, preprocessorConfig)
}

/**
  * Decorator for doing nothing -
  * should be used, when all features are already cached and we do not need term candidates occurrences
  *
  * @param innerPreprocessor for reading (tokenizing and lemmatizing) expected terms list
  */
class DummyNLPPreprocessor(innerPreprocessor: NLPPreprocessor) extends NLPPreprocessor {
  override def extractWords(text: String): Seq[Word] = innerPreprocessor.extractWords(text)
  override def preprocess(dirName: String) : DSDataset = null
}

/**
  * Configuration/builder for DummyNLPEnricher
  *
  * @param nlpConfig configuration of preprocessor, which should be cached
  */
case class DummyNLPPreprocessorConfig(nlpConfig: NLPPreprocessorConfig) extends NLPPreprocessorConfig {
  override def build(): NLPPreprocessor = new DummyNLPPreprocessor(nlpConfig.build())
}

object DummyNLPPreprocessorConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make(nlpConfig: NLPPreprocessorConfig) = DummyNLPPreprocessorConfig(nlpConfig)
}