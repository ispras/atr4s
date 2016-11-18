package ru.ispras.atr.features

import ru.ispras.atr.datamodel.{DSDataset, TermCandidate}

import scala.util.Random

/**
  * Returns random number.
  *
  * @param seed for random generator
  */
case class RandomFeature(seed: Long = 42) extends FeatureConfig {
  override def build(candidates: Seq[TermCandidate], dataset: DSDataset): FeatureComputer = {
    val random = new Random(seed)
    new RandomFC(random)
  }
}

class RandomFC(random: Random) extends FeatureComputer {
  override def compute(tc: TermCandidate): Double = {
    val res = random.nextDouble()
    res
  }
}

