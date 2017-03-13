package ru.ispras.atr.candidates

import org.scalatest.FunSuite
import ru.ispras.atr.datamodel._

class TermCandidatesCollectorTest extends FunSuite {

  val twoDocs = Seq(
    DSDocument("doc1",
//      "Information retrieval was researched by many good scientists",
      Seq(
        Word("information", "NN"),
        Word("retrieval", "NN"),
        Word("be", "VBD"),
        Word("research", "VBN"),
        Word("by", "IN"),
        Word("many", "JJ"),
        Word("good", "JJ"),
        Word("scientist", "NNS"),
        Word(".", ".")
      )),
    DSDocument("doc2",
//      "Thus information retrieval is appropriate field.",
      Seq(
        Word("thus", "CC"),
        Word("information", "NN"),
        Word("retrieval", "NN"),
        Word("be", "VBZ"),
        Word("appropriate", "JJ"),
        Word("field", "NN"),
        Word(".", ".")
      ))
  )
  val twoDocsDataset = new DSDataset(twoDocs.map(d => (d.name, d)).toMap)

  val information_retrieval = Seq(
    TermCandidate(Seq(
      TermOccurrence(
        Seq("information", "retrieval"),
        twoDocs.head.name,
        0),
      TermOccurrence(
        Seq("information", "retrieval"),
        twoDocs.last.name,
        1)
    )
    )
  )

  test("testCollectFreqBigram") {
    val actual: Seq[TermCandidate] = TCCConfig(nGramSizes=Seq(2), minTermFreq=2).build().collect(twoDocsDataset)
    val expected = information_retrieval
    assert(expected === actual)
  }

  test("testCollectAllBigram") {
    val actual = TCCConfig(nGramSizes=Seq(2), minTermFreq=1).build().collect(twoDocsDataset).toSet
    val expected = (information_retrieval :+
      TermCandidate(Seq(
        TermOccurrence(
          Seq("good", "scientist"),
          twoDocs.head.name,
          6)
      ))).toSet
    //Note that "appropriate field" should be filtered by stop words checker
    assert(expected === actual)
  }

  test("testCollectUnigram") {
    val actual = TCCConfig(nGramSizes=Seq(1), minTermFreq=2).build().collect(twoDocsDataset).toSet
    val expected = Set(
      TermCandidate(Seq(
        TermOccurrence(
          Seq("information"),
          twoDocs.head.name,
          0),
        TermOccurrence(
          Seq("information"),
          twoDocs.last.name,
          1)
      )),
        TermCandidate(Seq(
          TermOccurrence(
            Seq("retrieval"),
            twoDocs.head.name,
            1),
          TermOccurrence(
            Seq("retrieval"),
            twoDocs.last.name,
            2)
        )
      ))
    assert(expected === actual)
  }
}
