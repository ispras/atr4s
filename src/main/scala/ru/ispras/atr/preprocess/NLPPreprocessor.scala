package ru.ispras.atr.preprocess

import java.io.File

import org.apache.logging.log4j.{LogManager, Logger}
import ru.ispras.atr.datamodel.{DSDataset, DSDocument, Word}

import scala.io.{Codec, Source}

/**
  * Preprocessed input documents: firstly, reads them from the specified directory, then
  * splits input text documents into sentences,
  * tokenizes obtained sentences,
  * and finds part of speech tags and lemmas for obtained tokens.
  */
trait NLPPreprocessor {
  val log: Logger = LogManager.getLogger(getClass)

  def preprocess(dirName: String) : DSDataset = {
    log.debug(s"Reading files from $dirName")
    val d = new File(dirName)
    val files: Array[File] = if (d.exists && d.isDirectory) {
      d.listFiles.filter(!_.getName.endsWith(".dat"))
    } else {
      throw new IllegalArgumentException(s"passed directory name ($dirName) is invalid")
    }
    if (files.length == 0) {
      throw new IllegalArgumentException(s"passed directory contains no documents")
    }
    //read all files in memory, since we anyway have to store all docs for feature computation
    val namedTexts: Array[(String, String)] = files.map(f => {
      val source = Source.fromFile(f)(codec())
      val text = try
        source.getLines.mkString("\n")
        catch {
          case mie: java.nio.charset.MalformedInputException =>
            log.error(s"Input text files should be encoded in ${codec()}; file with wrong encoding: ${f.getName}")
            throw mie
          case e: Throwable => throw e
        }
      finally source.close()
      (f.getName.replace(".txt", ""), text)
    })
    log.debug(s"Preprocessing ${namedTexts.length} texts")
    preprocess(namedTexts)
  }

  def preprocess(texts: Seq[(String, String)]) : DSDataset = {
    DSDataset(texts.par.map(namedText => {
      val name = namedText._1
      val text = namedText._2
      (name, new DSDocument(name, extractWords(text)))
    }).seq.toMap)
  }

  def extractWords(text: String): Seq[Word]

  def codec(): Codec = Codec.UTF8
}

/**
  * Configuration/builder for [[ru.ispras.atr.preprocess.NLPPreprocessor Preprocessor]]
  */
trait NLPPreprocessorConfig {
  def build(): NLPPreprocessor
}

object NLPPreprocessorConfig {
  val subclasses = List(classOf[OpenNLPPreprocessorConfig],
    classOf[EmoryNLPPreprocessorConfig],
    classOf[CachingNLPPreprocessorConfig],
    classOf[DummyNLPPreprocessorConfig])
}