package ru.ispras.atr.preprocess

import edu.emory.mathcs.nlp.decode.NLPDecoder
import ru.ispras.atr.datamodel.Word
import scala.collection.JavaConversions.asScalaBuffer

/**
  * Note that preliminary experiments show drop of 1-5\% in average precision in case of switching to Emory nlp4j,
  * mainly because errors of part of speech tagging.
  *
  * However, note that Stanford CoreNL is distributed under GPL,
  * while Emory nlp4j is licensed under the Apache License, Version 2.0.
  *
  * See https://emorynlp.github.io/nlp4j
  *
  * @param decoder should be read from xml-file with config, see [[ru.ispras.atr.preprocess.EmoryNLPPreprocessorConfig]]
  */
class EmoryNLPPreprocessor(decoder: NLPDecoder) extends NLPPreprocessor {

  override def extractWords(text: String): Seq[Word] = {
    val sents = decoder.decodeDocument(text)
    //take "tail" here, because EmoryNLP seems to add special token to the head (I don't know why)
    sents.flatMap(_.tail.map(node => new Word(node.getLemma, node.getPartOfSpeechTag))
    )
  }
}

case class EmoryNLPPreprocessorConfig(configPath: String = "/emorynlp_config_pos.xml") extends NLPPreprocessorConfig {
  override def build(): NLPPreprocessor = {
    val stream = getClass.getResourceAsStream(configPath)
    val decoder = new NLPDecoder(stream)
    new EmoryNLPPreprocessor(decoder)
  }
}

object EmoryNLPPreprocessorConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = EmoryNLPPreprocessorConfig()
}