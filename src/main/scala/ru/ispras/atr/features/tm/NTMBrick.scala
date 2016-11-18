package ru.ispras.modis.tm.chinesetm

import org.apache.logging.log4j.LogManager
import ru.ispras.modis.tm.attribute.AttributeType
import ru.ispras.modis.tm.brick.AbstractPLSABrick
import ru.ispras.modis.tm.documents.Document
import ru.ispras.modis.tm.matrix.{AttributedPhi, Background, Theta}
import ru.ispras.modis.tm.regularizer.Regularizer
import ru.ispras.modis.tm.sparsifier.Sparsifier
import ru.ispras.modis.tm.utils.ModelParameters

/**
  * Created with IntelliJ IDEA.
  * User: padre
  * Date: 21.05.14
  * Time: 18:12
  */
class NTMBrick(regularizer: Regularizer,
               phiSparsifier: Sparsifier,
               attribute: AttributeType,
               modelParameters: ModelParameters,
               private val documentFrequency: Array[Float],
               val background: Background,
               val noise: Background,
               private val noisePrior: Float,
               private val backgroundPrior: Float)
  extends AbstractPLSABrick(regularizer, phiSparsifier, attribute, modelParameters, 1f) {
  // TODO make a class DocumentFrequency

  val log = LogManager.getLogger(getClass)

  var n = 0L
  var tw = 0d
  var bw = 0d
  var nw = 0d

  def makeIteration(theta: Theta, phi: AttributedPhi, documents: Seq[Document], iterationCnt: Int): Double = {
    val entropy = Entropy(documents: Seq[Document], phi: AttributedPhi, theta: Theta)
    var logLikelihood = 0d
    for (document <- documents) {
      logLikelihood += processSingleDocument(document, theta, phi, entropy)
    }

    applyRegularizer(theta, phi)
    backgroundRegularizer(background, backgroundPrior)
    backgroundRegularizer(noise, noisePrior)

    background.dump()
    noise.dump()
    phi.dump()
    phi.sparsify(phiSparsifier, iterationCnt)

    log.debug("topicsWeight=" + tw / n + " backgroundWeight=" + bw / n + " noiseWeight=" + nw / n + "\n")
    logLikelihood
  }

  private def processSingleDocument(document: Document, theta: Theta, phi: AttributedPhi, entropy: Entropy): Double = {
    var logLikelihood = 0d
    for ((wordIndex, numberOfWords) <- document.getAttributes(attribute)) {
      logLikelihood += processOneWord(wordIndex, numberOfWords, document.serialNumber, phi, theta, entropy)
    }
    logLikelihood
  }

  private def processOneWord(wordIndex: Int, numberOfWords: Short, documentIndex: Int, phi: AttributedPhi, theta: Theta, entropy: Entropy): Double = {
    // TODO replace weight by appropriate formula
    val noiseWeight = (1f - entropy(wordIndex, documentIndex)) * (1f - documentFrequency(wordIndex) / theta.numberOfRows)
    val backgroundWeight = entropy(wordIndex, documentIndex)
    val topicsWeight = 1f - noiseWeight - backgroundWeight

    n += 1
    tw += topicsWeight
    bw += backgroundWeight
    nw += noiseWeight

    if (!(noiseWeight >= 0)) {
      log.debug(wordIndex)
      log.debug("phi row " +(wordIndex, 0.until(phi.numberOfTopics).map(topicId => phi.probability(topicId, wordIndex)).sum))
      log.debug("theta row " +(documentIndex, 0.until(theta.numberOfTopics).map(topicId => theta.probability(documentIndex, topicId)).sum))

      def updateOneCell(phi: AttributedPhi, theta: Theta, documentIndex: Int, wordIndex: Int): Float = {
        val topicIndexes = 0.until(phi.numberOfRows).toArray
        val denominator = topicIndexes.foldLeft(0d) { (sum, topicIndex) =>
          calculateC(phi, theta, documentIndex, wordIndex, topicIndex) + sum
        }

        log.debug("denominator " + denominator)

        val result = topicIndexes.foldLeft(0d) { (sum, topicIndex) =>
          val c = calculateC(phi, theta, documentIndex, wordIndex, topicIndex)
          println("c " + c)
          sum + c / denominator * Math.log(c / denominator)
        }

        -(result / Math.log(phi.numberOfRows)).toFloat
      }


      def calculateC(phi: AttributedPhi, theta: Theta, documentIndex: Int, wordIndex: Int, topicIndex: Int) = {
        phi.probability(topicIndex, wordIndex) * theta.probability(documentIndex, topicIndex)
      }

      log.debug(updateOneCell(phi, theta, documentIndex, wordIndex))
    }

    require(noiseWeight >= 0, "entropy(wordIndex) = " + entropy(wordIndex, documentIndex) + " documentFrequency(wordIndex) " + documentFrequency(wordIndex))
    require(backgroundWeight >= 0, "entropy(wordIndex) = " + entropy(wordIndex, documentIndex))
    require(topicsWeight >= 0)

    val z = (topicsWeight * countZ(phi, theta, wordIndex, documentIndex)
      + backgroundWeight * background.probability(wordIndex)
      + noiseWeight * noise.probability(wordIndex))

    var topic = 0
    while (topic < modelParameters.numberOfTopics) {
      val ndwt = numberOfWords * theta.probability(documentIndex, topic) * phi.probability(topic, wordIndex) / z
      theta.addToExpectation(documentIndex, topic, ndwt)
      phi.addToExpectation(topic, wordIndex, ndwt)
      topic += 1
    }
    background.addToExpectation(wordIndex, numberOfWords * background.probability(wordIndex) * backgroundWeight / z)
    background.addToExpectation(wordIndex, numberOfWords * noise.probability(wordIndex) * noiseWeight / z)
    numberOfWords * math.log(z)
  }

  private def backgroundRegularizer(background: Background, parameter: Float) {
    0.until(background.numberOfWords).foreach(wordIndex => background.addToExpectation(wordIndex, parameter))
  }
}

object NTMBrick {

  private def getDocumentFrequence(documents: Seq[Document], modelParameters: ModelParameters, attribute: AttributeType) = {
    val result = new Array[Float](modelParameters.numberOfWords(attribute))
    documents.filter(doc => doc.contains(attribute)).foreach(doc => doc.getAttributes(attribute).foreach {
      case (wordIndex, wordNumber) => result(wordIndex) += 1
    })
    result
  }

  def apply(modelParameters: ModelParameters,
            attribute: AttributeType,
            regularizer: Regularizer,
            phiSparsifier: Sparsifier,
            documents: Seq[Document],
            noisePrior: Float,
            backgroundPrior: Float) = {

    val documentFrequency = getDocumentFrequence(documents, modelParameters, attribute)
    val background = Background(attribute, modelParameters)
    val noise = Background(attribute, modelParameters)

    new NTMBrick(regularizer, phiSparsifier, attribute, modelParameters, documentFrequency, background, noise, noisePrior, backgroundPrior)
  }
}
