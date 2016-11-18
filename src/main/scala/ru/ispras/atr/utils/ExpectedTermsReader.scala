package ru.ispras.atr.utils

import org.apache.logging.log4j.LogManager
import ru.ispras.atr.datamodel.TermOccurrence
import ru.ispras.atr.preprocess.NLPPreprocessor

/**
  * Reads and normalizes (lemmatizes) expected terms from the file.
  */
object ExpectedTermsReader {
  val log = LogManager.getLogger(getClass)
  //TODO add caching
  def apply(path: String, enricher: NLPPreprocessor): Set[String] = {
    val source = scala.io.Source.fromFile(path)
    val termSet = source.getLines().map(line => {
      val words = enricher.extractWords(line)
      TermOccurrence.canonicalRepresentation(words.map(_.lemma))
    }).toSet
    log.debug(s"Expected terms: ${termSet.size}")
    termSet
  }
//  val enricher = EmoryNLPPreprocessorConfig().build()
//  apply("patents_terms.txt", enricher)
//  apply("genia_terms.txt", enricher)
//  apply("krapivin_n_comp_merged_terms.txt", enricher)
//  apply("fao_terms.txt", enricher)
//  apply("acl2_terms.txt", enricher)
//  apply("acl_terms.txt", enricher)
//  apply("eurovoc_thesaurus.txt", enricher)
}
