package ru.ispras.modis.tm.chinesetm

import ru.ispras.modis.tm.attribute.AttributeType
import ru.ispras.modis.tm.documents.Document
import ru.ispras.modis.tm.matrix.{AttributedPhi, Theta}
import ru.ispras.modis.tm.plsa.PLSA
import ru.ispras.modis.tm.regularizer.Regularizer
import ru.ispras.modis.tm.sparsifier.Sparsifier
import ru.ispras.modis.tm.stoppingcriteria.StoppingCriteria

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 22.05.14
 * Time: 18:01
 */
class NTMWall(bricks: Map[AttributeType, NTMBrick],
              stoppingCriteria: StoppingCriteria,
              thetaSparsifier: Sparsifier,
              regularizer: Regularizer,
              documents: Seq[Document],
              phi: Map[AttributeType, AttributedPhi],
              theta: Theta)
    extends PLSA(bricks, stoppingCriteria, thetaSparsifier, regularizer, documents, phi, theta) {

    override def train: NTMTrainedModel = {
        val collectionLength = documents.foldLeft(0) {
            (sum, document) => sum + document.numberOfWords()
        }
        var numberOfIteration = 0
        var oldPpx = 0d
        var newPpx = 0d
        while (!stoppingCriteria(numberOfIteration, oldPpx, newPpx)) {
            oldPpx = newPpx
            newPpx = makeIteration(numberOfIteration: Int, collectionLength: Int, documents: Seq[Document])
            numberOfIteration += 1
        }

        val background = bricks.map{case(attribute, brick) => (attribute, brick.background)}
        val noise = bricks.map{case(attribute, brick) => (attribute, brick.noise)}
        new NTMTrainedModel(phi, theta, noise, background, newPpx)
    }
}
