package ru.ispras.modis.tm.chinesetm

import ru.ispras.modis.tm.documents.Document
import ru.ispras.modis.tm.matrix.{AttributedPhi, Theta}
import scala.math.{log, max}

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 22.05.14
 * Time: 14:40
 */
class Entropy(private val values: Array[Map[Int, Float]]) {
    def apply(wordIndex: Int, documentIndex: Int) = values(documentIndex)(wordIndex)
}

object Entropy {
    def apply(documents: Seq[Document], phi: AttributedPhi, theta: Theta) = {
        val values = documents.map(document => processOneDocument(document, phi, theta)).toArray
        new Entropy(values)
    }


    private def processOneDocument(document: Document, phi: AttributedPhi, theta: Theta): Map[Int, Float] = {
         document.getAttributes(phi.attribute).map{case(word, num) =>
             word -> updateOneCell(phi, theta: Theta, document.serialNumber, word)
         }.toMap
    }

    private def updateOneCell(phi: AttributedPhi, theta: Theta, documentIndex: Int, wordIndex: Int): Float = {
        val topicIndexes = 0.until(phi.numberOfRows).toArray
        val denominator = topicIndexes.foldLeft(0d){(sum, topicIndex) =>
            calculateC(phi, theta, documentIndex, wordIndex, topicIndex) + sum
        }

        val result = topicIndexes.foldLeft(0d){(sum, topicIndex) =>
            val c = calculateC(phi, theta, documentIndex, wordIndex, topicIndex)
            sum + c / denominator * log(c / denominator)
        }

        -(result / log(phi.numberOfRows)).toFloat
    }


    private def calculateC(phi: AttributedPhi, theta: Theta, documentIndex: Int, wordIndex: Int, topicIndex: Int) = {
        max(Float.MinPositiveValue, phi.probability(topicIndex, wordIndex) * theta.probability(documentIndex, topicIndex))
    }
}
