package ru.ispras.atr.candidates

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.datamodel.{DSDataset, DataConfig, TermCandidate}
import ru.ispras.atr.preprocess.NLPPreprocessorConfig
import ru.ispras.atr.utils.Cacher

/**
  * Decorator for caching results of TermCandidatesCollector
  *
  * @param cacher helper that works with cache
  */
class CachingTermCandidatesCollector(cacher: Cacher[TermCandidatesCollectorConfig, Seq[TermCandidate], DSDataset])
  extends TermCandidatesCollector(null, 0, null) {

  override def collect(dataset: DSDataset): Seq[TermCandidate] = {
    val result = cacher.getFromCache(dataset)
    log.debug(s"Total term candidates: ${result.size}")
    result
  }
}

/**
  * Configuration/builder for CachingTermCandidatesCollector
  *
  * @param dataConfig   configuration of data to be preprocessed
  * @param nlpConfig    configuration of preprocessor to be used for candidates collection
  * @param tccConfig    configuration of term candidates collector, which should be cached
  * @param cacheDirName name of subdirectory, where this cache will be stored
  */
case class CachingTCCConfig(dataConfig: DataConfig,
                            nlpConfig: NLPPreprocessorConfig,
                            tccConfig: TCCConfig,
                            cacheDirName: String = "candidates/"
                           ) extends TermCandidatesCollectorConfig{
  val log = LogManager.getLogger(getClass)
  override def build() = {

    def recreateObject(dataset: DSDataset): Seq[TermCandidate] = {
      val initializedDataset = if (dataset == null || dataset.docsMap.head == null) {
        log.debug(s"Reinitializing dataset...")
        nlpConfig.build().preprocess(dataConfig.docsDir)
      } else dataset
      val innerTCC = tccConfig.build()
      innerTCC.collect(initializedDataset)
    }

    new CachingTermCandidatesCollector(
      new Cacher[TermCandidatesCollectorConfig, Seq[TermCandidate], DSDataset](this, cacheDirName, recreateObject))
  }
}

object CachingTCCConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make(dataConfig: DataConfig, nlpConfig: NLPPreprocessorConfig, tccConfig: TCCConfig) =
    CachingTCCConfig(dataConfig, nlpConfig, tccConfig)

  val subclasses = List(classOf[CachingTCCConfig])
}

/**
  * Decorator for caching only names of term candidates -
  * should be used, when all features are already cached and we do not need term candidates occurrences
  */
class NamesOnlyTermCandidatesCollector extends TermCandidatesCollector(null, 0, null) {
  override def collect(dataset: DSDataset): Seq[TermCandidate] = null
}

/**
  * Configuration/builder for NamesOnlyTermCandidatesCollector
  *
  * @param tccConfig configuration of term candidates collector
  * @param cacheDirName name of subdirectory, where this cache will be stored
  */
case class NamesOnlyTCCConfig(tccConfig: TermCandidatesCollectorConfig,
                              cacheDirName: String = "candidateNames/") extends TermCandidatesCollectorConfig {
  val log = LogManager.getLogger(getClass)
  override def build(): TermCandidatesCollector = {

    def recreateObject(dataset: DSDataset): Seq[TermCandidate] = {
      log.debug(s"Initializing term candidates (for names)...")
      val innerTCC = tccConfig.build()
      innerTCC.collect(dataset).map(tc => new TermCandidate(Seq(tc.occurrences.head)))
    }

    new CachingTermCandidatesCollector(
      new Cacher[TermCandidatesCollectorConfig, Seq[TermCandidate], DSDataset](this, cacheDirName, recreateObject))
  }
}

object NamesOnlyTCCConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make(tccConfig: TermCandidatesCollectorConfig) = NamesOnlyTCCConfig(tccConfig)

  val subclasses = List(classOf[NamesOnlyTCCConfig])
}