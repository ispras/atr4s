package ru.ispras.atr.utils

object VectorUtils {
  def normalizeL2(vector: Seq[Double]) = {
    val norm = Math.sqrt(vector.map(e => Math.pow(e, 2)).sum)
    if (norm < 1e-15) {
      vector
    } else {
      vector.map(e => e / norm)
    }
  }
}
