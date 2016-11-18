package ru.ispras.atr.features.contexts

import ru.ispras.atr.candidates.NoiseWordsCheckerConfig
import ru.ispras.atr.datamodel.{DSDataset, DSDocument, TermCandidate, TermOccurrence}
import ru.ispras.atr.features.occurrences.Basic
import ru.ispras.atr.features.{FeatureComputer, FeatureConfig}
import ru.ispras.atr.utils.MapUtils

/**
  * DomainCoherence works in 3 steps:
  * <li>First, it extracts predefined number of best term candidates by using Basic method.
  * <li>Then, words from contexts of previously extracted term candidates are filtered:
  * it keeps only nouns, adjectives, verbs and adverbs
  * that occur in at least predefined portion (1 quarter, by default) of all documents
  * and are similar to these term candidates, i.e. ranked in the top predefined number by averaged Normalized PMI.
  * <li>Finally, as a weight of a term candidate, DomainCohrerence takes the average of the same NPMI measures
  * computed with each of 50 context words extracted in the previous step.
  *
  * <br>Based on:
  * Bordea, G., Buitelaar, P., Polajnar, T.: Domain-independent term extraction through domain modelling.<br>
  * In: the 10th International Conference on Terminology and Artificial Intelligence (TIA 2013)(2013)<br>
  *
  * (NB: this is config/builder that only initializes corresponding FeatureComputer.)
  *
  * @param seedCandidatesCount  count of term candidates extracted at the first step
  * @param seedFeature          feature that ranks term candidates at the first step
  * @param relWordsCount        count of related words selected at the second step
  * @param posTagsCheckerConfig checks part-of-speech tag for related word candidate
  * @param relWordsPortion      in what portion of documents should a word appear to be related word candidate
  * @param contextSize          size of context (in words, to left and to right), from which related words are selected
  * @param pmi                  configuration for computing PMI
  */
case class DomainCoherence(seedCandidatesCount: Int = 200,
                           seedFeature: FeatureConfig = Basic(),
                           relWordsCount: Int = 50,
                           posTagsCheckerConfig: NoiseWordsCheckerConfig = NoiseWordsCheckerConfig(NoiseWordsCheckerConfig.semanticBearingPoSes),
                           relWordsPortion: Double = 0.25,
                           contextSize: Int = 5,
                           pmi: PMI = PMI()
                          ) extends FeatureConfig {

  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val posTagsChecker = posTagsCheckerConfig.build()
    val goodPoSWords2DocFreq = dataset.docsMap.values.par.map(doc => {
      val allGoodPoSWords: Set[String] = doc.words.filter(w => posTagsChecker.isGoodPoS(w.posTag)).map(_.lemma).toSet
      val allGoodPoSWordsMap = allGoodPoSWords.toSeq.map(rw => rw -> 1).toMap
      allGoodPoSWordsMap
    }).reduce((m1: Map[String, Int], m2: Map[String, Int]) => MapUtils.mergeMaps(m1, m2)).seq

    val relWordCandidatesCount = dataset.docsMap.size * 0.25
    val relWordCandidates = goodPoSWords2DocFreq.filter(_._2 >= relWordCandidatesCount).keys.toSet

    //compute freqs of relWordCandidates
    val relWordFreqs: Map[String, Int] = dataset.docsMap.values.par.map(doc => {
      doc.words.map(_.lemma).filter(relWordCandidates.contains).groupBy(x => x).mapValues(_.size)
    }).reduce((m1: Map[String, Int], m2: Map[String, Int]) => MapUtils.mergeMaps(m1, m2)).seq

    val corpusSize = dataset.sizeInWords

    //extract seed terms
    val seedFC = seedFeature.build(candidates, dataset)
    val seedTerms = candidates.par.map(tc => (tc, seedFC.compute(tc))).seq.sortBy(_._2).map(_._1).take(seedCandidatesCount)

    val term2RelWords2Freq: Map[TermCandidate, Map[String, Int]] = seedTerms.par.map(st => {
      st -> RelatedWordsInContextFrequency(st, relWordCandidates, dataset, contextSize)
    }).seq.toMap//.reduce((map1,map2) => mergeMaps(map1,map2)).seq

    //compute PMI between relwordcandidates and seedTerms
    val relWord2PMI = relWordCandidates.par.map(rwc => {
      val totalPMI = seedTerms.map(s => {
        pmi(relWordFreqs(rwc), s.freq, term2RelWords2Freq(s).getOrElse(rwc, 0), corpusSize)
      }).sum
      rwc -> totalPMI
    }).seq

    //take relwords with top PMI
    val topRelWords = relWord2PMI.toSeq.sortBy(_._2).take(relWordsCount).map(_._1).toSet

    val relatedWords2Freq = relWordFreqs.filterKeys(topRelWords.contains)

    new DomainCoherenceFC(relatedWords2Freq, corpusSize, contextSize, dataset, pmi)
  }
}

class DomainCoherenceFC(relatedWords2Freq: Map[String, Int],
                        corpusSize: Long,
                        contextSize: Int,
                        dataset: DSDataset,
                        pmi: PMI
                       ) extends FeatureComputer {

  override def compute(tc: TermCandidate): Double = {
    val curSeedRelWordsFreq = RelatedWordsInContextFrequency(tc, relatedWords2Freq.keys.toSet, dataset, contextSize)
    relatedWords2Freq.map(rwWithFreq => {
      pmi(rwWithFreq._2, tc.freq, curSeedRelWordsFreq.getOrElse(rwWithFreq._1, 0), corpusSize)
    }).sum
  }
}

object RelatedWordsInContextFrequency {

  def context(to: TermOccurrence, dataset: DSDataset, size: Int): Seq[String] = {
    val startContextIndex = Math.max(0, to.startWordIndex - size)
    val doc = dataset.docsMap(to.docName)
    val endContextIndex = Math.min(to.endWordIndex + size + 1, doc.words.size)
    (doc.words.slice(startContextIndex, to.startWordIndex) ++ doc.words.slice(to.endWordIndex + 1, endContextIndex))
      .map(_.lemma)
  }

  def apply(term: TermCandidate, relatedWords: Set[String], dataset: DSDataset, contextSize: Int): Map[String, Int] = {
    val curSeedContextRelWords = term.occurrences.par.flatMap(to =>
      context(to, dataset, contextSize).filter(relatedWords.contains))
    curSeedContextRelWords.groupBy(x => x).mapValues(_.size).seq.toMap
  }
}

/**
  * Computes (normalized) PMI with modifications proposed in
  * Levy, Omer, Yoav Goldberg, and Ido Dagan.
  * "Improving distributional similarity with lessons learned from word embeddings."
  * Transactions of the Association for Computational Linguistics 3 (2015): 211-225.
  *
  * @param laplasSmoothing         constant used in case of no co-occurrence (i.e. contextFreq = 0)
  * @param k                       constant used to act 'as a prior on the probability of observing a positive example
  *                                (an actual occurrence of (w;c) in the corpus) versus a negative example;
  *                                a higher k means that negative examples are more probable'
  * @param contextDistribSmoothing constant used to 'smooth the original contexts’ distribution,
  *                                all context counts are raised to the power of alpha [this value]'
  * @param positive                indicates if the result PMI should be positive (i.e. max(PMI,0) or not
  */
case class PMI(laplasSmoothing: Double = 1e-75,
               k: Int = 1,
               contextDistribSmoothing: Double = 1,
               positive: Boolean = false) {
  def apply(relWordFreq: Int, termFreq: Int, contextFreq: Int, corpusSize: Long) = {
    val pSigma = relWordFreq.toDouble // /corpusSize
    val pTetta = termFreq.toDouble // /corpusSize
    val pSigmaTetta = if (contextFreq == 0) laplasSmoothing else contextFreq.toDouble // /corpusSize
    val pmi = if (contextDistribSmoothing == 1) { //for optimization: in order to not compute pow(x,1)
        //multiply nominator by corpusSize, because should divide all 3 parts (pSigmaTetta, pSigma, pTetta) by corpusSize
        Math.log(pSigmaTetta * corpusSize / (pSigma * pTetta))
      } else {
        Math.log(pSigmaTetta / pTetta * Math.pow(corpusSize / pSigma, contextDistribSmoothing))
      }
    val shiftedPMI = pmi - Math.log(k)
    val positiveShiftedPMI = if (positive) Math.max(shiftedPMI, 0) else shiftedPMI
    positiveShiftedPMI / -Math.log(pSigmaTetta / corpusSize)
  }
}