package ru.ispras.atr.preprocess

import opennlp.tools.lemmatizer.{DictionaryLemmatizer, SimpleLemmatizer}
import opennlp.tools.postag.{POSModel, POSTagger, POSTaggerME}
import opennlp.tools.sentdetect.{SentenceDetector, SentenceDetectorME, SentenceModel}
import opennlp.tools.tokenize.{Tokenizer, TokenizerME, TokenizerModel}
import ru.ispras.atr.datamodel.Word

/**
  * Preprocesses texts by using Apache OpenNLP.
  *
  * (Preliminary experiments show that its quality is slightly worse than Stanford NLP;
  * moreover, it is not thread-safe)
  *
  *  See http://opennlp.apache.org/index.html
  */
class OpenNLPPreprocessor(sentdet: SentenceDetector,
                          tokenizer: Tokenizer,
                          tagger: POSTagger,
                         lemmatizer: DictionaryLemmatizer
) extends NLPPreprocessor {

  override def extractWords(text: String): Seq[Word] = synchronized {
    val sents: Array[String] = sentdet.sentDetect(text)
    sents.flatMap(s => {
      val tokens = tokenizer.tokenize(s)
      val poses = tagger.tag(tokens)
      (tokens zip poses).map(tp => {
        val lemma = lemmatizer.lemmatize(tp._1, tp._2)
        new Word(lemma, tp._2)
      })
    })
  }
}

case class OpenNLPPreprocessorConfig(sentModPath: String = "/en-sent.bin",
                                     tokenModPath: String = "/en-token.bin",
                                     posModPath: String = "/en-pos-maxent.bin",
                                     lemDictPath: String = "/en-lemmas.dict") extends NLPPreprocessorConfig {
  override def build(): NLPPreprocessor = {
    val sentmod = new SentenceModel(getClass.getResource(sentModPath))
    val tokmod = new TokenizerModel(getClass.getResource(tokenModPath))
    val posmod = new POSModel(getClass.getResource(posModPath))

    val sentdet: SentenceDetector = new SentenceDetectorME(sentmod)
    val tokenizer: Tokenizer = new TokenizerME(tokmod)
    val tagger: POSTagger = new POSTaggerME(posmod)
    val lemmatizer = new SimpleLemmatizer(getClass.getResource(lemDictPath).openStream())

    new OpenNLPPreprocessor(sentdet,  tokenizer, tagger, lemmatizer)
  }
}

object OpenNLPPreprocessorConfig {
  /** constructor for Java, since it doesn't support parameters with default values */
  def make() = OpenNLPPreprocessorConfig()
}