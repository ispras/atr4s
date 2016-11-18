package ru.ispras.atr.features

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.candidates.TermCandidatesCollectorConfig
import ru.ispras.atr.datamodel.{DSDataset, DataConfig, TermCandidate}
import ru.ispras.atr.preprocess.NLPPreprocessorConfig
import ru.ispras.atr.utils.Cacher

/**
  * Decorator for caching results of FeatureComputer
  *
  * @param cacher helper that works with cache
  */
class CachingFeatureComputer(cacher: Cacher[CachingFeature, Seq[Double], Seq[TermCandidate]]) extends FeatureComputer {
  val log = LogManager.getLogger(getClass)

  override def compute(candidates: Seq[TermCandidate]): Seq[Double] = {
    cacher.getFromCache(candidates)
  }

  override def compute(tc: TermCandidate): Double =
    throw new RuntimeException("Caching feature computer can be used in batch-mode only, not for single term candidate")
}

/**
  * Configuration/builder for CachingFeatureComputer.
  * Note that it also enables reinitialization of preprocessed dataset and candidates if it is needed.
  *
  * @param dataConfig   configuration of data to be preprocessed
  * @param nlpConfig    configuration of preprocessor to be used for candidates collection
  * @param tccConfig    configuration of term candidates collector
  * @param innerFeature configuration of inner feature computer, which should be cached
  * @param cacheDirName name of subdirectory, where this cache will be stored
  */
case class CachingFeature(dataConfig: DataConfig,
                          nlpConfig: NLPPreprocessorConfig,
                          tccConfig: TermCandidatesCollectorConfig,
                          innerFeature: FeatureConfig,
                          cacheDirName: String = "features/"
                         ) extends FeatureConfig {
  override def id: String = innerFeature.id

  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    def recreateObject(originalParams: Seq[TermCandidate]): Seq[Double] = {
      log.debug(s"Initializing feature ${innerFeature.id}...")
      val initializedDataset = if (dataset == null || dataset.docsMap.head == null) {
        log.debug(s"Reinitializing dataset...")
        nlpConfig.build().preprocess(dataConfig.docsDir)
      } else dataset
      val initializedCandidates = if (candidates == null || candidates.head.occurrences.size <= 1) {
        //strictly, 1 occurrence doesn't necessarily mean that candidates weren't initialized,
        // but since only for small datasets it is reasonable to disable frequency filtering, reinitialization isn't bad
        log.debug(s"Reinitializing candidates...")
        tccConfig.build().collect(initializedDataset)
      } else candidates
      val featureComputer = innerFeature.build(initializedCandidates, initializedDataset)
      log.debug(s"Computing feature ${innerFeature.id}...")
      featureComputer.compute(initializedCandidates)
    }

    new CachingFeatureComputer(
      new Cacher[CachingFeature, Seq[Double], Seq[TermCandidate]](this, cacheDirName, recreateObject))
  }
}