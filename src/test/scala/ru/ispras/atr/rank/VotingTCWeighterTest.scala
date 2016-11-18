package ru.ispras.atr.rank

import org.scalatest.{BeforeAndAfterEach, FunSuite}
import ru.ispras.atr.TestDataProvider


class VotingTCWeighterTest extends FunSuite with BeforeAndAfterEach {

  val df = TestDataProvider.sqlContext.read.format("com.databricks.spark.csv").
    option("header", "true").
    option("inferSchema", "true").
    load(getClass.getResource("/dfVoting.csv").toString)

  test("testWeight") {
    df.show()
    val votingDF = new VotingTCWeighter(null).weight(df)
    votingDF.show()

    val terms = votingDF.rdd.map(r => (r(0).asInstanceOf[String],r(1).asInstanceOf[Double])).collect()
    val expected = Seq(("android", 1.5),
      ("misspeling", 2.0/3), //3rd rank by both features
      ("something", 2))
    val actualSorted = terms.sortBy(t => t._1)
    assert(expected === actualSorted)
  }

}
