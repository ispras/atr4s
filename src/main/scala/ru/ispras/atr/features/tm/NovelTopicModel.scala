package ru.ispras.atr.features.tm

import java.util.Random

import ru.ispras.atr.candidates.NoiseWordsCheckerConfig
import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}
import ru.ispras.modis.tm.chinesetm.{NTMTrainedModel, NTMWallBuilder}
import ru.ispras.modis.tm.documents.{Alphabet, SingleAttributeNumerator}
import ru.ispras.modis.tm.matrix.Background
import ru.ispras.modis.tm.utils.TopicHelper

/**
  * Based on the idea that distribution of words over topics found by topic modeling is a less noisy signal
  * than simple frequency of occurrences.
  *
  * <br>Based on:
  *   Li, S., Li, J., Song, T., Li, W., & Chang, B. (2013, July).
  *   A novel topic model for automatic term extraction.
  *   In Proceedings of the 36th international ACM SIGIR conference on Research and development in information retrieval
  *   (pp. 885-888). ACM.
  *
  * Default constants are also taken from the paper, see equation (10).
  */
case class NovelTopicModel(noiseWordsCheckerConfig: NoiseWordsCheckerConfig = NoiseWordsCheckerConfig(),
                           minWordsInDocCount: Int = 2,
                           topicsCount: Int = 20,
                           noisePrior: Float = 0.01f, //betta2
                           backgroundPrior: Float = 0.01f, //betta1
                           topicPrior: Float = 0.1f, //betta0
                           topicWeightPriorNominator: Float = 50f, //alpha = 50/numberOfTopics-1
                           iterationsCount: Int = 100,
                           rareWordsThreshold: Int = 1, //word should occur at least (rareWordsThreshold+1) times
                           randomSeed: Long = 13,
                           topWordsForTopic: Int = 200 //top H
                          ) extends FeatureConfig {

  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val topicWeightPrior = topicWeightPriorNominator / topicsCount - 1
    val noiseWordsChecker = noiseWordsCheckerConfig.build()
    val inputDocs = dataset.docsMap.values
      .map(_.words.filter(noiseWordsChecker.isGoodWord).map(_.lemma))
      .filter(_.size >= minWordsInDocCount)
      .iterator

    val input = SingleAttributeNumerator(inputDocs, rareWordsThreshold)

    val docs = input._1
    val alphabet = input._2

    val random = new Random(randomSeed)
    val chineseLDA = new NTMWallBuilder(
      topicsCount,
      alphabet,
      docs,
      random,
      iterationsCount,
      noisePrior,
      backgroundPrior,
      topicPrior,
      topicWeightPrior).build()
    val model: NTMTrainedModel = chineseLDA.train
    val typicalWord2Index = extractTypicalWord2Index(model, alphabet)
    val topicModel = TypicalWordsTopicModel(model, typicalWord2Index, topicsCount)
    new NovelTopicModelFC(topicModel)
  }

  def extractTypicalWord2Index(model: NTMTrainedModel, alphabet: Alphabet) = {
    val mainTopicsWordIndices: Set[Int] = (0 until topicsCount)
      .flatMap(topicIndex => TopicHelper.getTopWords(model.getPhi, topicIndex, topWordsForTopic).toSet).toSet
    val backgroundWordIndices: Set[Int] = getTopBackgroundWords(topWordsForTopic, model.getBackground(), alphabet).toSet
    val docSpecificWordIndices: Set[Int] = getTopBackgroundWords(topWordsForTopic, model.getNoise(), alphabet).toSet

    mainTopicsWordIndices.union(backgroundWordIndices).union(docSpecificWordIndices)
      .map(topWordIndex => alphabet(topWordIndex) -> topWordIndex).toMap
  }

  //taken from old TopicHelper
  def getTopBackgroundWords(n: Int, background: Background, alphabet: Alphabet) =
    0.until(background.numberOfWords).map(ind =>(ind, background.probability(ind))).sortBy(-_._2).take(n).map(_._1)
}

case class TypicalWordsTopicModel(model: NTMTrainedModel,
                                  typicalWord2Index: Map[String, Int],
                                  topicsCount: Int) {
  val missedValue = 0f

  def maxProb(word: String) = {
    (topicProbs(word) :+ backgroundProb(word) :+ dosSpecificProb(word)).max
  }

  def topicProbs(word: String): Seq[Float] =
    typicalWord2Index.get(word) match {
      case None => Seq(missedValue)
      case Some(wordIndex) => (0 until topicsCount).map(topicIndex => model.getPhi.probability(topicIndex, wordIndex))
    }
  def backgroundProb(word: String): Float = matrixProb(word, model.getBackground())
  def dosSpecificProb(word: String): Float = matrixProb(word, model.getNoise())

  def matrixProb(word: String, matrix: Background) =
    typicalWord2Index.get(word) match {
      case None => missedValue
      case Some(wordIndex) => matrix.probability(wordIndex)
    }
}

class NovelTopicModelFC(topicModel: TypicalWordsTopicModel) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val words = tc.lemmas
    val probsSum = words.map(topicModel.maxProb).sum

    val logTFi = Math.log(tc.occurrences.size)
    logTFi * probsSum
  }
}