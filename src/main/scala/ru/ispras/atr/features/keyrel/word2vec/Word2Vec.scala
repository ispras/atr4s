package ru.ispras.atr.features.keyrel.word2vec

import scala.collection.mutable
import spire.algebra._
import spire.implicits._

// Copyright 2014 astanton
// Derived work from:
// Copyright 2013 trananh
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/** A Scala port of the word2vec model.  This interface allows the user to access the vector representations
  * output by the word2vec tool, as well as perform some common operations on those vectors.  It does NOT
  * implement the actual continuous bag-of-words and skip-gram architectures for computing the vectors.
  *
  * More information on word2vec can be found here: https://code.google.com/p/word2vec/
  *
  * Example usage:
  * {{{
  * val model = new Word2Vec()
  * model.load("vectors.bin")
  * val results = model.distance(List("france"), N = 10)
  *
  * model.pprint(results)
  * }}}
  *
  * @constructor Create a word2vec model.
  *
  * @author trananh
  */
class Word2Vec(vocab: Map[String, Array[Float]], vecSize: Int, normalized: Boolean) {

  /** Number of words */
  private val numWords = vocab.size

  /** Return the number of words in the vocab.
    * @return Number of words in the vocab.
    */
  def wordsCount: Int = numWords

  /** Size of the vectors.
    * @return Size of the vectors.
    */
  def vectorSize: Int = vecSize

  /** Check if the word is present in the vocab map.
    * @param word Word to be checked.
    * @return True if the word is in the vocab map.
    */
  def contains(word: String): Boolean = vocab.contains(word)

  /** Get the vector representation for the word.
    * @param word Word to retrieve vector for.
    * @return The vector representation of the word.
    */
  def vector(word: String): Option[Array[Float]] = vocab.get(word)

  /** Compute the Euclidean distance between two vectors.
    * @param vec1 The first vector.
    * @param vec2 The other vector.
    * @return The Euclidean distance between the two vectors.
    */
  def euclidean(vec1: Array[Float], vec2: Array[Float]): Double = {
    (vec1 - vec2).map(spire.math.pow(_,2)).sum.sqrt
  }

  /** Compute the Euclidean distance between the vector representations of the words.
    * @param word1 The first word.
    * @param word2 The other word.
    * @return The Euclidean distance between the vector representations of the words.
    */
  def euclidean(word1: String, word2: String): Option[Double] = {
    traverseVectors(List(word1, word2)) collectFirst {
      case v1 :: v2 :: Nil => euclidean(v1, v2)
    }
  }

  /** Compute the cosine similarity score between two vectors.
    * @param vec1 The first vector.
    * @param vec2 The other vector.
    * @return The cosine similarity score of the two vectors.
    */
  def cosine(vec1: Array[Float], vec2: Array[Float]): Double = {
    assert(vec1.length == vec2.length, "Uneven vectors!")
    val dot = vec1 dot vec2
    if (normalized) {
      dot
    } else {
      val sum1 = vec1 dot vec1
      val sum2 = vec2 dot vec2
      dot / (sum1.sqrt * sum2.sqrt)
    }
  }

  /** Compute the cosine similarity score between the vector representations of the words.
    * @param word1 The first word.
    * @param word2 The other word.
    * @return The cosine similarity score between the vector representations of the words.
    */
  def cosine(word1: String, word2: String): Option[Double] = {
    traverseVectors(List(word1, word2)) collectFirst {
      case v1 :: v2 :: Nil => cosine(v1, v2)
    }
  }

  /** Find the vector representation for the given list of word(s) by aggregating (summing) the
    * vector for each word.
    * @param vecs .
    * @return The sum vector (aggregated from the input vectors).
    */
  private def sumVector(vecs: List[Array[Float]]): Array[Float] = {
    // Find the vector representation for the input. If multiple words, then aggregate (sum) their vectors.
    vecs reduce {_+_}
  }

  /** Find N closest terms in the vocab to the given vector, using only words from the in-set (if defined)
    * and excluding all words from the out-set (if non-empty).  Although you can, it doesn't make much
    * sense to define both in and out sets.
    * @param vector The vector.
    * @param inSet Set of words to consider. Specify None to use all words in the vocab (default behavior).
    * @param outSet Set of words to exclude (default to empty).
    * @param N The maximum number of terms to return (default to 40).
    * @return The N closest terms in the vocab to the given vector and their associated cosine similarity scores.
    */
  def nearestNeighbors(vector: Array[Float], inSet: Option[Set[String]] = None,
                       outSet: Set[String] = Set[String](), N: Integer = 40)
  : List[(String, Float)] = {

    def items = for(s <- inSet) yield {
      s.toStream collect { case k if vocab.contains(k) => k -> vocab(k) }
    }

    def it = items.getOrElse(vocab.toStream) collect {
      case (k, v) if !outSet.contains(k) => k -> cosine(vector, v).toFloat
    }

    // Scala needs to have an immutable pq.  I should write one
    val top = new mutable.PriorityQueue[(String, Float)]()(Ordering.by(-_._2))
    it.zipWithIndex.foldLeft(top) {
      case (agg, ((k, dist), i)) if i < N => agg += k -> dist
      case (agg, ((k, dist), i)) if agg.head._2 < dist => {
        agg.dequeue()
        agg += k -> dist
      }
      case (agg, _) => agg
    }

    // Return the top N results as a sorted list.
    assert(top.length <= N)
    top.toList.sortWith(_._2 > _._2)
  }

  /** Find the N closest terms in the vocab to the input word(s).
    * @param input The input word(s).
    * @param N The maximum number of terms to return (default to 40).
    * @return The N closest terms in the vocab to the input word(s) and their associated cosine similarity scores.
    */
  def distance(input: List[String], N: Integer = 40): Option[List[(String, Float)]] = {

    // Find the vector representation for the input. If multiple words, then aggregate (sum) their vectors.
    input match {
      case Nil => Some(List[(String, Float)]())
      case _ => traverseVectors(input) map { vecs =>
        nearestNeighbors(sumVector(vecs).normalize, outSet=input.toSet, N=N)
      }
    }
  }

  /** Find the N closest terms in the vocab to the analogy:
    * - [word1] is to [word2] as [word3] is to ???
    *
    * The algorithm operates as follow:
    * - Find a vector approximation of the missing word = vec([word2]) - vec([word1]) + vec([word3]).
    * - Return words closest to the approximated vector.
    *
    * @param word1 First word in the analogy [word1] is to [word2] as [word3] is to ???.
    * @param word2 Second word in the analogy [word1] is to [word2] as [word3] is to ???
    * @param word3 Third word in the analogy [word1] is to [word2] as [word3] is to ???.
    * @param N The maximum number of terms to return (default to 40).
    *
    * @return The N closest terms in the vocab to the analogy and their associated cosine similarity scores.
    */
  def analogy(word1: String, word2: String, word3: String, N: Integer = 40): Option[List[(String, Float)]] = {
    traverseVectors(List(word1, word2, word3)) collectFirst {
      case v1 :: v2 :: v3 :: Nil => {
        val vector = v2 - v1 + v3
        nearestNeighbors(vector.normalize, outSet = Set(word1, word2, word3), N = N)
      }
    }
  }

  /** Rank a set of words by their respective distance to some central term.
    * @param word The central word.
    * @param set Set of words to rank.
    * @return Ordered list of words and their associated scores.
    */
  def rank(word: String, set: Set[String]): List[(String, Float)] = {
    vocab.get(word).map({
      v => nearestNeighbors(v, inSet = Some(set), N = set.size)
    }).getOrElse(List())
  }

  /** Pretty print the list of words and their associated scores.
    * @param words List of (word, score) pairs to be printed.
    */
  def pprint(words: List[(String, Float)]) = {
    println("\n%50s".format("Word") + (" " * 7) + "Cosine distance\n" + ("-" * 72))
    println(words.map(s => "%50s".format(s._1) + (" " * 7) + "%15f".format(s._2)).mkString("\n"))
  }

  def traverseVectors(words: List[String]): Option[List[Array[Float]]] = {
    val vecs = words.foldLeft(Option(List[Array[Float]]())) {
      case (agg, word) => agg.flatMap(lst => vector(word).map(v => v :: lst))
    }
    vecs map {_.reverse}
  }

}

object Word2Vec {
  /** Load data from a binary file.
    * @param filename Path to file containing word projections in the BINARY FORMAT.
    * @param limit Maximum number of words to load from file (a.k.a. max vocab size).
    * @param normalize Normalize the loaded vectors if true (default to true).
    */
  def apply(filename: String, normalize: Boolean = true, limit: Integer = Int.MaxValue): Option[Word2Vec] = {
    for(v <- VecBinaryReader.load(filename, limit, normalize)) yield {
      new Word2Vec(v.vectors, v.size, normalize)
    }
  }

}


/** ********************************************************************************
  * Demo of the Scala ported word2vec model.
  * ********************************************************************************
  */
object RunWord2Vec {

  /** Demo. */
  def main(args: Array[String]) {
    // Load word2vec model from binary file.
    val model = Word2Vec("../word2vec-scala/vectors.bin").get

    // distance: Find N closest words
    model.pprint(model.distance(List("france"), N = 10).get)
    model.pprint(model.distance(List("france", "usa")).get)
    model.pprint(model.distance(List("france", "usa", "usa")).get)

    // analogy: "king" is to "queen", as "man" is to ?
    model.pprint(model.analogy("king", "queen", "man", N = 10).get)

    // rank: Rank a set of words by their respective distance to the central term
    model.pprint(model.rank("apple", Set("orange", "soda", "lettuce")))
  }

}

