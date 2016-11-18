package ru.ispras.atr.features.keyrel.word2vec

import java.io._

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
case class Vocab(vectors: Map[String, Array[Float]], size: Int)

trait TypeReader[A] {
  def read(dis: DataInputStream): A
}
object TypeReader {
  def apply[A: TypeReader] = implicitly[TypeReader[A]]

  implicit object ByteReader extends TypeReader[Byte] {
    def read(dis: DataInputStream): Byte = dis.readByte()
  }

  implicit def readerToStream[A:TypeReader] = new TypeReader[Stream[A]] {
    def read(dis: DataInputStream): Stream[A] = TypeReader[A].read(dis) #:: read(dis)
  }

  implicit object StringReader extends TypeReader[String] {
    /** ASCII values for common delimiter characters */
    private val SPACE = 32
    private val LF = 10
    private val DELIMS = Set(SPACE, LF)
    def read(dis: DataInputStream): String = {
      val s = TypeReader[Stream[Byte]].read(dis).takeWhile(!DELIMS.contains(_))
      new String(s.toArray[Byte])
    }
  }

  implicit object FloatReader extends TypeReader[Float] {
    def read(dis: DataInputStream): Float = {
      val i = java.lang.Integer.reverseBytes(dis.readInt())
      java.lang.Float.intBitsToFloat(i)
    }
  }

  implicit object IntReader extends TypeReader[Int] {
    def read(dis: DataInputStream): Int = TypeReader[String].read(dis).toInt
  }


}

/** A simple binary file reader.
  *
  * @constructor Create a binary file reader.
  * @param file The binary file to be read.
  * @author trananh
  */
class VecBinaryReader(file: File) {

  /** Open input streams */
  private val fis = new FileInputStream(file)
  private val bis = new BufferedInputStream(fis)
  private val dis = new DataInputStream(bis)

  /** Close the stream. */
  def close() { dis.close(); bis.close(); fis.close() }

  def read[A:TypeReader] = TypeReader[A].read(dis)

}

object VecBinaryReader {
  def apply(file: File):VecBinaryReader = new VecBinaryReader(file)
  def apply(filename: String): VecBinaryReader = apply(new File(filename))

  def loadFile(filename: String): Option[File] = {
    val file = new File(filename)
    if(!file.exists()) None else Some(file)
  }

  def withReader[A](file: File)(f: VecBinaryReader => A): A = {
    val reader = VecBinaryReader(file)
    try {
      f(reader)
    } finally {
      reader.close()
    }
  }

  /** Compute the magnitude of the vector.
    *
    * @param vec The vector.
    * @return The magnitude of the vector.
    */
  def magnitude(vec: Array[Float]): Double = {
    math.sqrt(vec.toStream.map(a => a * a).sum)
  }

  def normVector(vec: Array[Float]): Array[Float] = {
    val norm = magnitude(vec)
    vec.map(a => (a / norm).toFloat)
  }

  def readVector(reader: VecBinaryReader, vecSize:Int, normalize: Boolean): (String, Array[Float]) = {
    // Read the word
    val word = reader.read[String]

    val vector = new Array[Float](vecSize)
    for((f, i) <- reader.read[Stream[Float]].take(vecSize).zipWithIndex) {
      vector(i) = f
    }

    // Modern tools like gensim stores model without delimiter character after last element of each vector
//      // Eat up the next delimiter character
//      reader.read[Byte]

    // Store the normalized vector representation, keyed by the word
    word -> (if (normalize) normVector(vector) else vector)
  }

  def load(filename: String, limit: Integer = Int.MaxValue, normalize: Boolean = true): Option[Vocab] = {
    for(file <- loadFile(filename)) yield VecBinaryReader.withReader(file) { reader =>
      // Read header info
      val numWords = reader.read[Int]
      val vecSize = reader.read[Int]
      println("\nFile contains " + numWords + " words with vector size " + vecSize)

      def wordPairs = Stream.continually(readVector(reader, vecSize, normalize))
      val N = numWords.min(limit)

      val map = wordPairs.take(N).toMap

      println("Loaded " + math.min(numWords, limit) + " words.\n")
      Vocab(map, vecSize)
    }
  }
}
