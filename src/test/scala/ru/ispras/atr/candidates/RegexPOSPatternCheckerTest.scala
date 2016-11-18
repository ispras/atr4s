package ru.ispras.atr.candidates

import org.scalatest.FunSuite

class RegexPOSPatternCheckerTest extends FunSuite {

  val checker = new RegexPOSPatternCheckerConfig().build()

  test("testSatisfyUnigram") {
    assert(checker.satisfy(Seq("NN")))
    assert(checker.satisfy(Seq("NNS")))

    assert(!checker.satisfy(Seq("JJ")))
    assert(!checker.satisfy(Seq("IN")))
    assert(!checker.satisfy(Seq("VB")))
    assert(!checker.satisfy(Seq("NNP")))
    assert(!checker.satisfy(Seq("NNPS")))
  }

  test("testSatisfyBigram") {
    assert(checker.satisfy(Seq("NN", "NN")))
    assert(checker.satisfy(Seq("NN", "NNS")))
    assert(checker.satisfy(Seq("NNS", "NN")))
    assert(checker.satisfy(Seq("NNS", "NNS")))
    assert(checker.satisfy(Seq("JJ", "NN")))
    assert(checker.satisfy(Seq("JJ", "NNS")))

    assert(!checker.satisfy(Seq("JJ", "JJ")))
    assert(!checker.satisfy(Seq("IN", "NN")))
    assert(!checker.satisfy(Seq("PDT", "NN")))
//    assert(!checker.satisfy(Seq("NNP", "NN")))
  }

  test("testSatisfyTrigram") {
    assert(checker.satisfy(Seq("NN", "NN", "NNS")))
    assert(checker.satisfy(Seq("NN", "NN", "NN")))
    assert(checker.satisfy(Seq("NNS", "NN", "NN")))
    assert(checker.satisfy(Seq("NNS", "JJ", "NN")))
    assert(checker.satisfy(Seq("JJ", "NN", "NN")))

    assert(!checker.satisfy(Seq("JJ", "NN", "JJ")))
    assert(!checker.satisfy(Seq("IN", "NN", "NN")))
    assert(!checker.satisfy(Seq("PDT", "NN", "NN")))
//    assert(!checker.satisfy(Seq("NNP", "NN", "NN")))
  }
}
