package ru.ispras.atr.candidates

import org.scalatest.FunSuite

class StopWordsCheckerTest extends FunSuite {

  val checker = new StopWordsCheckerConfig().build()

  test("testSatisfyYes") {
    assert(checker.satisfy(Seq("information", "retrieval")))
  }

  test("testSatisfyNo") {
    assert(!checker.satisfy(Seq("second", "method")))
  }

  test("testSatisfyNoWithCase") {
    assert(!checker.satisfy(Seq("Second", "method")))
  }
}
