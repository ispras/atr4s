package ru.ispras.atr.features.keyrel

/**
  * Stems tokens into base form.
  */
trait Stemmer{
  def stem(token:String):String
}

/**
  * Does nothing, simply returns the same token.
  */
class NoStemmer() extends Stemmer{
  def stem(token:String):String = token
}

/**
  * Stems tokens into base form not so good as lemmatizer, but much faster; should be used for word2vec model.
  */
class SnowballStemmer() extends Stemmer{
  val stemmer = Class.forName("org.tartarus.snowball.ext.englishStemmer").newInstance().asInstanceOf[org.tartarus.snowball.SnowballStemmer]

  def stem(token: String): String = this.synchronized {
    stemmer.setCurrent(token.toLowerCase)
    stemmer.stem()
    stemmer.getCurrent
  }
}