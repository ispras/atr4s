package ru.ispras.modis.tm.chinesetm

import java.util.Random

import ru.ispras.modis.tm.attribute.AttributeType
import ru.ispras.modis.tm.builder.PLSABuilder
import ru.ispras.modis.tm.documents.{Alphabet, Document}
import ru.ispras.modis.tm.initialapproximationgenerator.RandomInitialApproximationGenerator
import ru.ispras.modis.tm.matrix.Theta
import ru.ispras.modis.tm.regularizer.SymmetricDirichlet
import ru.ispras.modis.tm.sparsifier.ZeroSparsifier
import ru.ispras.modis.tm.stoppingcriteria.MaxNumberOfIterationStoppingCriteria
import ru.ispras.modis.tm.utils.ModelParameters

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 22.05.14
 * Time: 18:20
 */
class NTMWallBuilder(numberOfTopics: Int,
                     alphabet: Alphabet,
                     documents: Seq[Document],
                     random: Random,
                     numberOfIteration: Int,
                     private val noisePrior: Float,
                     private val backgroundPrior: Float,
                     private val topicPrior: Float,
                     private val topicWeightPrior: Float)
    extends PLSABuilder(numberOfTopics, alphabet, documents, random, numberOfIteration) {

    initialApproximationGenerator = new RandomInitialApproximationGenerator(random)
    thetaSparsifier = new ZeroSparsifier()

    phiSparsifier = new ZeroSparsifier()

    stoppingCriteria = new MaxNumberOfIterationStoppingCriteria(numberOfIteration)

    regularizer = new SymmetricDirichlet(topicPrior, topicWeightPrior)

    stoppingCriteria = new MaxNumberOfIterationStoppingCriteria(numberOfIteration)

    override protected def brickBuilder(modelParameters: ModelParameters): Map[AttributeType, NTMBrick] = {
        modelParameters.numberOfWords.map {case (key, value) =>
                (key, NTMBrick(modelParameters, key, regularizer, phiSparsifier, documents, noisePrior, backgroundPrior))
        }
    }

    override def build(): NTMWall = {
        val bricks = brickBuilder(modelParameters)
        val (theta, phi) = initialApproximationGenerator(modelParameters, documents)
        new NTMWall(bricks,
            stoppingCriteria,
            thetaSparsifier,
            regularizer,
            documents,
            phi,
            theta: Theta)
    }
}
