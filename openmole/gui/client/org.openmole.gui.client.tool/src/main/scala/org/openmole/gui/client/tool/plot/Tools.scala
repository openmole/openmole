package org.openmole.gui.client.tool.plot
import scala.util.Try

object Tools {

  implicit def stringToDouble(seq: Array[String]): Array[Double] = Try(
    seq.map {
      _.toDouble
    }
  ).toOption.getOrElse(Array(0.0))

}
