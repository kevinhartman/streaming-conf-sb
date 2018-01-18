package mn.hart

import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import scala.collection.mutable

object ExampleStream {
  def apply(streamingContext: StreamingContext): DStream[String] = {
    def batch: RDD[String] = {
      val strings = List(
        "zero",
        "one",
        "two",
        "three",
        "four",
        "five",
        "six",
        "seven",
        "eight",
        "nine"
      )

      streamingContext.sparkContext.parallelize(strings)
    }

    val exampleRDDs = mutable.Queue(
      (0 to 99).toList.map(_ => batch) :_*
    )

    streamingContext.queueStream[String](exampleRDDs)
  }
}