package com.packt.spark.section3

import com.packt.spark._
import org.apache.spark._
import org.apache.spark.rdd._
import geotrellis.vector._

object SimpleIO extends ExampleApp {
  def run() =
    withSparkContext { implicit sc =>
      val neighborhoods = Neighborhoods.fromJson("data/Neighborhoods_Philadelphia.geojson")
      val bcNeighborhoods = sc.broadcast(neighborhoods)

      val neighborhoodsToViolations: RDD[(NeighborhoodData, Violation)] =
        fullDataset
          .mapPartitions { rows =>
            val parse = Violation.rowParser
            rows.flatMap { row => parse(row) }
          }
          .filter(_.ticket.fine > 5.0)
          .flatMap { violation =>
            val nb = bcNeighborhoods.value
            nb.find { case Feature(polygon, _) =>
              polygon.contains(violation.location)
            }.map { case Feature(_, neighborhood) =>
              (neighborhood, violation)
            }
          }

//      csvExample(neighborhoodsToViolations)
      objectFileExample(neighborhoodsToViolations)
    }

  def csvExample(neighborhoodsToViolations: RDD[(NeighborhoodData, Violation)])(implicit sc: SparkContext): Unit = {
    val path = "/Users/rob/tmp/TEST-OUT-SPARK-CSV"

    def write() = {
      val neighborhoodsToCounts: RDD[(NeighborhoodData, Int)] =
        neighborhoodsToViolations
          .mapValues(x => 1)
          .reduceByKey(_ + _)

      val csvLines =
        neighborhoodsToCounts
          .map { case (neighborhood, count) =>
            s""""${neighborhood.name}","$count""""
          }
          .coalesce(1)

      csvLines.saveAsTextFile(path)
    }

    def read() = {
      val readRdd = sc.textFile(path)
      println(s"${readRdd.count} records")
    }

    write()
//    read()
  }

  def objectFileExample(neighborhoodsToViolations: RDD[(NeighborhoodData, Violation)])(implicit sc: SparkContext): Unit = {
    val path = "file:/Users/rob/proj/packt/spark/test-data/TEST-OUT-SPARK-OBJ"

    def write() = {
      neighborhoodsToViolations.saveAsObjectFile(path)
    }

    def read() = {
      val readRdd = sc.objectFile[(NeighborhoodData, Violation)](path)
      println(s"${readRdd.count} records, ${readRdd.partitions.size} partitions")
    }

    read()
  }

}
