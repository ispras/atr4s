package ru.ispras.modis.tm.chinesetm

import java.io.{FileInputStream, FileOutputStream}

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}
import org.objenesis.strategy.StdInstantiatorStrategy
import ru.ispras.modis.tm.attribute.{DefaultAttributeType, AttributeType}
import ru.ispras.modis.tm.matrix.{AttributedPhi, Background, Theta}
import ru.ispras.modis.tm.plsa.TrainedModel

/**
 * Created with IntelliJ IDEA.
 * User: padre
 * Date: 22.05.14
 * Time: 17:52
 */
/**
 *
 * @param phi distribution of words by topic for every attribute
 * @param theta distribution of document by topic
 * @param noise distribution of specific  words (word that can't be interpret by any topic  finds oneself here).
 * @param background distribution of too common words (word that can be explain by any topic)
 * @param perplexity perplexity value obtained on the training of this model
 */
class NTMTrainedModel(phi: Map[AttributeType, AttributedPhi],
                      theta: Theta,
                      val noise: Map[AttributeType, Background],
                      val background: Map[AttributeType, Background],
                      perplexity: Double) extends TrainedModel(phi, theta, perplexity)  {
    def getNoise() = {
        require(noise.contains(DefaultAttributeType), "there is no default attribute in collection")
        require(noise.keys.size == 1, "Do not use this method in case of multiattribute collection")
        noise(DefaultAttributeType)
    }

    def getBackground() = {
        require(noise.contains(DefaultAttributeType), "there is no default attribute in collection")
        require(noise.keys.size == 1, "Do not use this method in case of multiattribute collection")
        noise(DefaultAttributeType)
    }
}

object NTMTrainedModel {
    def save(model: NTMTrainedModel, path: String) {
        val kryo = new Kryo
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy)
        val output = new Output(new FileOutputStream(path))
        kryo.writeObject(output, model)
        output.close()
    }

    def load(path: String) = {
        val kryo = new Kryo
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy)
        val input = new Input(new FileInputStream(path))
        val trainedModel = kryo.readObject(input, classOf[NTMTrainedModel])
        input.close()
        trainedModel
    }
}
