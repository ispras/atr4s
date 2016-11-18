package ru.ispras.atr.datamodel

/**
  * Trait for classed providing unique ids; it is needed mainly for Spark dataframe.
  */
trait Identifiable {

  def id: String = this.getClass.getSimpleName
}
