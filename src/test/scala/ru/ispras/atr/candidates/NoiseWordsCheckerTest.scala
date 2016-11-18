package ru.ispras.atr.candidates

import org.scalatest.FunSuite

class NoiseWordsCheckerTest extends FunSuite {

  val checker = new NoiseWordsCheckerConfig(minWordLength = 2).build()

  test("testSatisfyYes") {
    assert(checker.satisfy(Seq("information", "retrieval")))
  }

  test("testSatisfyNoShort") {
    assert(!checker.satisfy(Seq("second", "m")))
  }

  test("testSatisfyNoPunctuation") {
    assert(!checker.satisfy(Seq("second", "m!")))
    assert(!checker.satisfy(Seq("second", "?sdf")))
    assert(!checker.satisfy(Seq("second", "<sdf>")))
//    assert(!checker.satisfy(Seq("second", "asdf-a")))
  }

  //digits are ok (because of datasets like GENIA)
  test("testSatisfyNoDigit") {
    assert(checker.satisfy(Seq("second", "t1234")))
    assert(checker.satisfy(Seq("second", "1986")))
  }
}
