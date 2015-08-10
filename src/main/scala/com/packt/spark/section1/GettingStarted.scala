package com.packt.spark.section1

import com.packt.spark._
import org.apache.spark._

object GettingStarted extends ExampleApp {
  def run() =
    withSparkContext { implicit sc =>
      val rdd =
        fullDataset

      val count = rdd.count
      println(f"\nCount is ${count}%,d\n")

      waitForUser()
    }
}
