package ru.ispras.atr.features.keyrel

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.features.keyrel.word2vec.Word2Vec

import scala.collection.concurrent.TrieMap

/**
  * Configuration/builder for Word2VecAdapter
  */
trait Word2VecAdapterConfig {
  def build(): Word2VecAdapter
}

/**
  * Ordinary configuration/builder for Word2VecAdapter that actually reads model from the file
  *
  * @param w2vmodelPath path to word2vec model
  * @param normalize    boolean flag indicating if vectors should be normalized after downloading
  * @param missSimValue value of similarity that should be used
  *                     if one (or both) words/collocation are missed as wikipedia concepts
  */
case class NormWord2VecAdapterConfig(w2vmodelPath: String = "./data/w2vConcepts.model",
                                     normalize: Boolean = true,
                                     missSimValue: Double = 0
//                                    , redirectsPath: String = "data/wiki2vec/redirects_en.nt"
                                    ) extends Word2VecAdapterConfig {
  val log = LogManager.getLogger(getClass)
  override def build() = {
    log.debug("starting deserialization of w2v model")
    val start = System.nanoTime()
    val w2vModel = Word2Vec(w2vmodelPath, normalize).get
    log.debug(s"finished deserialization of w2v model (diff: ${(System.nanoTime() - start)/ 1e9})")
    new Word2VecAdapter(w2vModel, new SnowballStemmer(), missSimValue)
  }
}

/**
  * Configuration/builder for Word2VecAdapter that tries to take already read model.
  * Should be used for evaluation of multiple features that all use the same word2vec model
  *
  * @param innerConfig configuration that actually loads model (at the first time)
  */
case class CachedWord2VecAdapterConfig(innerConfig: NormWord2VecAdapterConfig = NormWord2VecAdapterConfig()) extends Word2VecAdapterConfig {
  override def build() = Word2VecAdapterCache.get(innerConfig)
}

/**
  * Encapsulates all logic related to Wikipedia nature of the word2vec model:
  * transformation of word/collocation to article title;
  * checking if word2vec model contains word/collocation;
  * computation of similarity of wikipedia concept and ordinary word/collocation
  *
  * @param w2vModel     word2vec model, which is assumed to contain wikipedia concepts in format "ID/word1_word2"
  * @param stemmer      stemmer used to transform words/collocation to normal form before searching in word2vec
  *                     (should be consistent with that used for word2vec model creation)
  * @param missSimValue value of similarity that should be used
  *                     if one (or both) words/collocation are missed as wikipedia concepts
  */
class Word2VecAdapter(w2vModel: Word2Vec,
//                      path2Redirects: String,
                      stemmer: Stemmer,
                      missSimValue: Double) {
  val log = LogManager.getLogger(getClass)

//  take MapRedirectStore from org.idio.wikipedia.redirects, but experiments have shown that redirects won't help
//  lazy val redirectStore = new MapRedirectStore(path2Redirects)

  def sim(firstWord: String, secondWord: String): Double = {
    w2vModel.cosine(stemmer.stem(firstWord), stemmer.stem(secondWord)).getOrElse(0)
  }

  def sim(firstVector: Array[Float], secondWord: String): Double = {
    w2vModel.vector(stemmer.stem(secondWord)) match {
      case None => missSimValue
      case Some(x) => w2vModel.cosine(firstVector, x)
    }
  }

  def toCanonicalRepr(words: Seq[String]) = {
//    "ID/" + redirectStore.getCanonicalId(words.mkString("_"))
    "ID/" + words.mkString("_")
  }

  def containsConcept(words: Seq[String]) = {
    w2vModel.contains(toCanonicalRepr(words))
  }

  def simConcepts(firstConcept: Seq[String], secondConcept: Seq[String]): Double = {
    w2vModel.cosine(toCanonicalRepr(firstConcept), toCanonicalRepr(secondConcept)).getOrElse(0)
  }

  def simConceptsWithFallback(firstVector: Array[Float], secondConcept: Seq[String]): Double = {
    w2vModel.vector(toCanonicalRepr(secondConcept)) match {
      case None => (getVectors(secondConcept).map(sv => w2vModel.cosine(firstVector, sv)) :+ 0.0).max //fall back to phrases
      case Some(x) => w2vModel.cosine(firstVector, x)
    }
  }

  def simConcepts(firstVector: Array[Float], secondConcept: Seq[String]): Double = {
    w2vModel.vector(toCanonicalRepr(secondConcept)) match {
      case None => missSimValue
      case Some(x) => w2vModel.cosine(firstVector, x)
    }
  }

  def getVectors(words: Seq[String]): Seq[Array[Float]] = {
    words.flatMap(w => w2vModel.vector(stemmer.stem(w)))
  }

  def getConceptVector(words: Seq[String]): Seq[Array[Float]] = {
    w2vModel.vector(toCanonicalRepr(words)) match {
      case None => Seq()
      case Some(x) => Seq(x)
    }
  }
}

/**
  * Singleton for in-memory cache implementation
  */
object Word2VecAdapterCache {
  val cache: TrieMap[NormWord2VecAdapterConfig, Word2VecAdapter] = TrieMap.empty

  def get(w2vConfig: NormWord2VecAdapterConfig) = {
      cache.getOrElseUpdate(w2vConfig, w2vConfig.build())
  }
}