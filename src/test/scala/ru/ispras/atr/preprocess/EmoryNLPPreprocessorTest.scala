package ru.ispras.atr.preprocess

import org.scalatest.FunSuite
import ru.ispras.atr.datamodel.{DSDocument, Word}

class EmoryNLPPreprocessorTest extends FunSuite {
  test("testPreprocess") {
    val text = "so, information retrieval was researched by many good scientists."
    val actualWords = EmoryNLPPreprocessorConfig().build().extractWords(text)
    val expectedWords = Seq(
      Word("so", "RB"),
      Word(",", ","),
      Word("information", "NN"),
      Word("retrieval", "NN"),
      Word("be", "VBD"),
      Word("research", "VBN"),
      Word("by", "IN"),
      Word("many", "JJ"),
      Word("good", "JJ"),
      Word("scientist", "NNS"),
      Word(".", ".")
    )
    assert(expectedWords === actualWords)
  }
}
