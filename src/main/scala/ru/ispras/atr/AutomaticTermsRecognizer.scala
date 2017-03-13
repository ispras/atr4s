package ru.ispras.atr

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import org.apache.log4j.LogManager
import ru.ispras.atr.candidates.{TermCandidatesCollector, TermCandidatesCollectorConfig, TCCConfig}
import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}
import ru.ispras.atr.features.occurrences.CValue
import ru.ispras.atr.features.refcorpus.Weirdness
import ru.ispras.atr.preprocess._
import ru.ispras.atr.rank.{OneFeatureTCWeighterConfig, TermCandidatesWeighter, TermCandidatesWeighterConfig}
import ru.ispras.atr.utils.JsonSer

/**
  * Facade for the whole ATR pipeline:
  * texts preprocessing, term candidates collection, term candidates scoring, and term candidates ranking.
  *
  * It assumes that input texts are domain-specific.
  *
  * @param nlpPreprocessor     performs input documents reading and texts preprocessing
  * @param candidatesCollector collects term candidates
  * @param candidatesWeighter  scores and ranks terms candidates
  */
class AutomaticTermsRecognizer(nlpPreprocessor: NLPPreprocessor,
                               candidatesCollector: TermCandidatesCollector,
                               candidatesWeighter: TermCandidatesWeighter) {
  val log = LogManager.getLogger(getClass)

  def preprocess(dirName: String): DSDataset = {
    nlpPreprocessor.preprocess(dirName)
  }

  def collectCandidates(dataset: DSDataset): Seq[TermCandidate] = {
    candidatesCollector.collect(dataset)
  }

  def weightAndSort(candidates: Seq[TermCandidate], dataset: DSDataset): Iterable[(String, Double)] = {
    candidatesWeighter.weightAndSort(candidates, dataset)
  }

  /**
    * Implements the main ATR pipeline:
    * (domain-specific) texts preprocessing,
    * term candidates collection,
    * term candidates scoring,
    * and term candidates ranking.
    *
    * @param dirName directory containing (domain-specific) texts
    * @param topCount count of top term candidates to return
    * @return term candidates with their weights sorted by this weight
    */
  def recognize(dirName: String, topCount: Int): Iterable[(String, Double)] = {
    val dataset = preprocess(dirName)
    val candidates = collectCandidates(dataset)
    val sortedTerms = weightAndSort(candidates, dataset)
    sortedTerms.take(topCount)
  }
}

/**
  * Configuration/builder for [[ru.ispras.atr.AutomaticTermsRecognizer AutomaticTermsRecognizer]]
  *
  * @param nlpPreprocessorConfig     config/builder for preprocessor
  * @param candidatesCollectorConfig config/builder for term candidates collector
  * @param candidatesWeighterConfig  config/builder for term candidates  scorer and ranker
  */
case class ATRConfig(nlpPreprocessorConfig: NLPPreprocessorConfig,
                     candidatesCollectorConfig: TermCandidatesCollectorConfig,
                     candidatesWeighterConfig: TermCandidatesWeighterConfig) {
  def build() = {
    new AutomaticTermsRecognizer(
      nlpPreprocessorConfig.build(),
      candidatesCollectorConfig.build(),
      candidatesWeighterConfig.build())
  }
}

/**
  * This and other objects are needed for type hints for JSON serialization.
  */
object ATRConfig {
  val subclasses = List(classOf[AutomaticTermsRecognizer]) ++
    NLPPreprocessorConfig.subclasses ++
    TermCandidatesCollectorConfig.subclasses ++
    TermCandidatesWeighterConfig.subclasses
}

/**
  * Supports the following arguments:
  * <li> 1. dataset directory
  * <li> 2. top count of best terms to be extracted
  * <li> 3. name of file containing JSON-serialized configuration of ATR
  * <li> 4. name of output file for recognized terms (if no, then terms are printed to the console)
  */
object AutomaticTermsRecognizer extends App {
  val log = LogManager.getLogger(getClass)

  if (args.length < 1)
    throw new RuntimeException("no dataset dir specified")
  val datasetDir = args(0)

  val topCount: Int = if (args.length > 1) {
    val chosenTopCount = args(1).toInt
    log.info(s"Using chosen topCount: $chosenTopCount")
    chosenTopCount
  } else
    throw new RuntimeException("no top count specified")

  val atrConfig = if (args.length > 2) {
    val atrConfFile = args(2)
    log.info(s"Using serialized ATR config: $atrConfFile")
    JsonSer.readFile[ATRConfig](atrConfFile)
  } else {
    log.info(s"Using default ATR config")
    val defaultConf = new ATRConfig(EmoryNLPPreprocessorConfig(), TCCConfig(), OneFeatureTCWeighterConfig(Weirdness()))
    //use this line to write a config into a file
//    JsonSer.writeFile[ATRConfig](defaultConf, "cvalue.conf")
    defaultConf
  }

  val atr = atrConfig.build()
  val terms = atr.recognize(datasetDir, topCount)
  if (args.length > 3) {
    val outFilename = args(3)
    println(outFilename)
    Files.write(Paths.get(outFilename), terms.mkString("\n").getBytes(StandardCharsets.UTF_8))
  } else {
    terms.foreach(println)
  }
}