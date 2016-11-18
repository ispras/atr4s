package ru.ispras.atr.utils

object MapUtils {
  def mergeMaps[T: Numeric](map1: Map[String, T], map2: Map[String, T])(implicit n: Numeric[T]): Map[String, T] = {
    //can be replaced by |+| from scalaz library
    map1 ++ map2.map { case (k, v) => k -> n.plus(v, map1.getOrElse(k, n.zero)) }
  }
}
