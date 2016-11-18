package ru.ispras.atr.preprocess

import java.io.File

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.datamodel.{DSDataset, DSDocument, Word}

import scala.io.Source

/**
  * Preprocessed input documents: firstly, reads them from the specified directory, then
  * splits input text documents into sentences,
  * tokenizes obtained sentences,
  * and finds part of speech tags and lemmas for obtained tokens.
  */
trait NLPPreprocessor {
  val log = LogManager.getLogger(getClass)

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
      val source = Source.fromFile(f)//, "ISO-8859-1") //hack for news20 texts
      val text = try source.getLines.mkString("\n") finally source.close()
//      //Hack for testing only annotations of krapivin
////      val s = text.indexOf("--A")
//      val e = text.indexOf("--B")
//      val restext = text.substring(0,e)
//      (f.getName.replace(".txt", ""), restext)
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
}

/**
  * Configuration/builder for [[ru.ispras.atr.preprocess.NLPPreprocessor Preprocessor]]
  */
trait NLPPreprocessorConfig {
  def build(): NLPPreprocessor
}

object NLPPreprocessorConfig {
  val subclasses = List(
    classOf[EmoryNLPPreprocessorConfig],
    classOf[CachingNLPPreprocessorConfig],
    classOf[DummyNLPPreprocessorConfig])
}