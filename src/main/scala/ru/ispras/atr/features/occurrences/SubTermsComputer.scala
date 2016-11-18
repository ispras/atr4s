package ru.ispras.atr.features.occurrences

import ru.ispras.atr.datamodel.TermCandidate

import scala.collection.immutable.IndexedSeq

/**
  * Computes maps of nesting between term candidates,
  * i.e. how many candidates the current candidate contains
  * or how many candidates are contained in the current candidate.
  * It is needed for featurss like [[ru.ispras.atr.features.occurrences.CValue]], [[ru.ispras.atr.features.occurrences.Basic]],
  * or [[ru.ispras.atr.features.occurrences.ComboBasic]].
  */
object SubTermsComputer {
  def computeShorter2longerTerms(candidates: Iterable[TermCandidate], minSubTermSize: Int) = {
    computeShorter2longerCollocations(candidates.map(_.lemmas), minSubTermSize)
  }

  def computeLonger2shorterTerms(candidates: Iterable[TermCandidate], minSubTermSize: Int = 1) = {
    computeLonger2shorterCollocations(candidates.map(_.lemmas), minSubTermSize)
  }

  def computeShorter2longerCollocations(candidates: Iterable[Seq[String]], minSubTermSize: Int) = {
    val candidateAndSubCollocations = computeCollocations2SubCollocations(candidates, minSubTermSize)
    val shorter2longerCollocations = new collection.mutable.HashMap[Seq[String], collection.mutable.Set[Seq[String]]]()
      with collection.mutable.MultiMap[Seq[String], Seq[String]]

    candidateAndSubCollocations.foreach(candAndSubCollocation =>
      candAndSubCollocation._2.foreach(st => {
        shorter2longerCollocations.addBinding(st, candAndSubCollocation._1)
      })
    )
    shorter2longerCollocations.toMap
  }

  def computeLonger2shorterCollocations(candidates: Iterable[Seq[String]], minSubTermSize: Int = 1) = {
    val candidateAndSubCollocations = computeCollocations2SubCollocations(candidates, minSubTermSize)
    val longer2shorterCollocations = new collection.mutable.HashMap[Seq[String], collection.mutable.Set[Seq[String]]]()
      with collection.mutable.MultiMap[Seq[String], Seq[String]]

    candidateAndSubCollocations.foreach(candAndSubCollocation =>
      candAndSubCollocation._2.foreach(st => {
        longer2shorterCollocations.addBinding(candAndSubCollocation._1, st)
      })
    )
    longer2shorterCollocations.toMap
  }

  def computeCollocations2SubCollocations(candidates: Iterable[Seq[String]], minSubTermSize: Int) = {
    val existedTermCandidateReprs = candidates.par.toSet

    val candidateAndSubTerms: Iterable[(Seq[String], IndexedSeq[Seq[String]])] = candidates.par.map(words => {
      val termSubWords = (minSubTermSize until words.size)
        .flatMap(l => words.sliding(l))
      val subTerms = termSubWords.filter(existedTermCandidateReprs.contains)
      (words, subTerms)
    }).seq
    candidateAndSubTerms
  }
}