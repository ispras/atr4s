package ru.ispras.atr.features.keyrel

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}

/**
  * algorithm for KeyConceptsRelatedness feature computation is the following:
  * <li>1. Extract key concepts for the whole document collection (see [[ru.ispras.atr.features.keyrel.KeyVectorsExtractor]])
  * <li>2. For each term candidate: if the word embedding model does not contain the term candidate,
  * then return 0;
  * otherwise compute semantic relatedness to extracted N key concepts
  * by weighted kNN adapted for the case with only positive instances.
  *
  * (NB: this is config/builder that only initializes corresponding FeatureComputer.)
  *
  * @param docKeysExtractorConfig specifies how to extract key concepts from docs
  * @param totalKeyConceptsCount  number of key concepts to extract
  * @param nearestKeysCount       number of key concepts to consider in weighted kNN
  * @param minKeyTotalFreq        minimum frequency of concept occurrence to be considered as a key candidate
  * @param topCountPerDoc         number of key concepts to be extracted from one document
  * @param w2vConfig              specifies word2vec adapter used for similarity computing
  */
case class KeyConceptRelatedness(docKeysExtractorConfig: DocKeysExtractorConfig = DocKeysExtractorConfig(),
                                 totalKeyConceptsCount: Int = 500,
                                 nearestKeysCount: Int = 2,
                                 minKeyTotalFreq: Int = 1,
                                 topCountPerDoc: Int = 15,
                                 w2vConfig: Word2VecAdapterConfig = CachedWord2VecAdapterConfig()) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val word2VecAdapter = w2vConfig.build()
    val keysExtractor = new KeyVectorsExtractor(docKeysExtractorConfig, totalKeyConceptsCount, topCountPerDoc,
      minKeyTotalFreq, true, word2VecAdapter)
    val keysVectors = keysExtractor.extract(dataset)
    new KeyConceptRelatednessFC(keysVectors, nearestKeysCount, word2VecAdapter)
  }
}

object KeyConceptRelatedness {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = KeyConceptRelatedness()
}

/**
  * The same as [[ru.ispras.atr.features.keyrel.KeyConceptRelatedness]], but instead of key concepts,
  * i.e. words/collocations that correspond to Wikipedia article, ordinary words are considered.
  *
  * In order to get one value from similarity of multiword collocations, aggregate function is applied.
  * Currently "max", "mean", "powerMean" are supported.
  *
  * (Experiments showed that it works worse than [[ru.ispras.atr.features.keyrel.KeyConceptRelatedness]])
  *
  * @param docKeysExtractorConfig specifies how to extract key phrases from docs
  * @param totalKeyPhrasesCount   number of key phrases to extract
  * @param nearestKeysCount       number of key phrases to consider in weighted kNN
  * @param topCountPerDoc         number of key phrases to be extracted from one document
  * @param minKeyTotalFreq        minimum frequency of phrase occurrence to be considered as a key candidate
  * @param aggregateFuncName      used to get one value from similarity of multiword collocations.
  *                               Currently "max", "mean", "powerMean" are supported.
  * @param power                  used only if powerMean was used in aggregateFuncName - means used power
  * @param w2vConfig              specifies word2vec adapter used for similarity computing
  */
case class KeyPhraseRelatedness(docKeysExtractorConfig: DocKeysExtractorConfig = DocKeysExtractorConfig(),
                                totalKeyPhrasesCount: Int = 200,
                                nearestKeysCount: Int = 5,
                                topCountPerDoc: Int = 5,
                                minKeyTotalFreq: Int = 2,
                                aggregateFuncName: String = "max",
                                power: Int = 0,
                                w2vConfig: Word2VecAdapterConfig = CachedWord2VecAdapterConfig()) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val word2VecAdapter = w2vConfig.build()
    val keysExtractor = new KeyVectorsExtractor(docKeysExtractorConfig, totalKeyPhrasesCount, topCountPerDoc,
      minKeyTotalFreq, false, word2VecAdapter)
    val keysVectors = keysExtractor.extract(dataset)

    val aggregateFunc = aggregateFuncName match {
      case "max" => (s: Seq[Double]) => s.max
      case "mean" => (s: Seq[Double]) => s.sum / s.size
      case "powerMean" => (s: Seq[Double]) => {
        val underSqrt = s.map(Math.pow(_,power)).sum / s.size
        if (underSqrt < 0) {
          -Math.pow(-underSqrt, 1.0/power)
        } else {
          Math.pow(underSqrt, 1.0/power)
        }
      }
    }

    new KeyPhraseRelatednessFC(keysVectors, nearestKeysCount, aggregateFunc, word2VecAdapter)
  }
}

/**
  * Combination of [[ru.ispras.atr.features.keyrel.KeyConceptRelatedness]] and
  * [[ru.ispras.atr.features.keyrel.KeyPhraseRelatedness]]:
  * if term candidate has corresponding concept in word2vec model, then use KeyConceptRelatedness;
  * otherwise -- KeyPhraseRelatedness.
  *
  * (Experiments showed that it works worse than [[ru.ispras.atr.features.keyrel.KeyConceptRelatedness]])
  *
  * @param docKeysExtractorConfig specifies how to extract key concepts from docs
  * @param totalKeyConceptsCount  number of key concepts to extract
  * @param nearestKeysCount       number of key concepts to consider in weighted kNN
  * @param topCountPerDoc         number of key concepts to be extracted from one document
  * @param minKeyTotalFreq        minimum frequency of phrase occurrence to be considered as a key candidate
  * @param w2vConfig              specifies word2vec adapter used for similarity computing
  */
case class KeyConceptRelatednessFallback(docKeysExtractorConfig: DocKeysExtractorConfig = DocKeysExtractorConfig(),
                                         totalKeyConceptsCount: Int = 500,
                                         nearestKeysCount: Int = 2,
                                         minKeyTotalFreq: Int = 2,
                                         topCountPerDoc: Int = 15,
                                         w2vConfig: Word2VecAdapterConfig = CachedWord2VecAdapterConfig()) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val word2VecAdapter = w2vConfig.build()
    val keysExtractor = new KeyVectorsExtractor(docKeysExtractorConfig, totalKeyConceptsCount, topCountPerDoc,
      minKeyTotalFreq, true, word2VecAdapter)
    val keysVectors = keysExtractor.extract(dataset)
    new KeyConceptRelatednessWithFallbackFC(keysVectors, nearestKeysCount, word2VecAdapter)
  }
}

object KeyConceptRelatednessFallback {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = KeyConceptRelatednessFallback()
}


abstract class KeysRelatednessFC(keysVectors: Seq[Seq[Array[Float]]],
                                 nearestKeysCount: Int) extends FeatureComputer{

  def sim(keyVectors: Seq[Array[Float]], secondPhrase: Seq[String]): Double

  override def compute(tc: TermCandidate): Double = {
    val candidatePhrase = tc.lemmas
    //weighted kNN, where k = nearestKeysCount
    keysVectors.map(kv => sim(kv, candidatePhrase))
      .sorted(Ordering[Double].reverse).take(nearestKeysCount)
      .sum / nearestKeysCount
    // actually, we don't have to normalize value, but it allows to have more interpretable value
  }
}

class KeyPhraseRelatednessFC(keysVectors: Seq[Seq[Array[Float]]],
                             nearestKeysCount: Int,
                             aggregateFunc: Seq[Double] => Double,
                             word2VecAdapter: Word2VecAdapter) extends KeysRelatednessFC(keysVectors, nearestKeysCount) {
  def sim(keyVector: Array[Float], secondWord: String): Double = {
    word2VecAdapter.sim(keyVector, secondWord)
  }

  override def sim(keyPhraseVectors: Seq[Array[Float]], secondPhrase: Seq[String]): Double = {
    val allPairsSims: Seq[Double] = keyPhraseVectors.flatMap(kv => secondPhrase.map(sp => sim(kv,sp)))
    aggregateFunc(allPairsSims)
  }
}

class KeyConceptRelatednessFC(keysVectors: Seq[Seq[Array[Float]]],
                              nearestKeysCount: Int,
                              val word2VecAdapter: Word2VecAdapter) extends KeysRelatednessFC(keysVectors, nearestKeysCount) {

  override def sim(keyConceptVectors: Seq[Array[Float]], secondPhrase: Seq[String]): Double = {
    assert(keyConceptVectors.size == 1)
    word2VecAdapter.simConcepts(keyConceptVectors.head, secondPhrase)
  }
}

class KeyConceptRelatednessWithFallbackFC(keysVectors: Seq[Seq[Array[Float]]],
                                          nearestKeysCount: Int,
                                          word2VecAdapter: Word2VecAdapter) extends KeysRelatednessFC(keysVectors, nearestKeysCount) {

  override def sim(keyConceptVectors: Seq[Array[Float]], secondPhrase: Seq[String]): Double = {
    assert(keyConceptVectors.size == 1)
    word2VecAdapter.simConceptsWithFallback(keyConceptVectors.head, secondPhrase)
  }
}

object KeyPhraseRelatedness {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = KeyPhraseRelatedness()

  val subclasses = List(
    classOf[KeyPhraseRelatedness],
    classOf[KeyConceptRelatedness],
    classOf[KeyConceptRelatednessFallback]) ++
    DocKeysExtractorConfig.subclasses
}