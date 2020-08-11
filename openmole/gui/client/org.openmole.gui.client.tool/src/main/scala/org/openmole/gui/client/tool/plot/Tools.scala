package org.openmole.gui.client.tool.plot

import scala.util.Try

import scala.scalajs.js.RegExp

object Tools {

  implicit def stringToDouble(seq: Array[String]): Array[Double] = Try(
    seq.map {
      _.toDouble
    }
  ).toOption.getOrElse(Array(0.0))

  val arrayRegEx = new RegExp("\\[[0-9].*,*\\]")

  def isDataArray(value: String) = {
    arrayRegEx.test(value)
  }

  def getDataArrays(dataCol: Seq[String]) = {
    dataCol.map { d ⇒
      arrayRegEx.exec(d).map { _.get }.head.tail.dropRight(1).split(',').toSeq
    }
  }

  def isOneColumnTemporal(data: Seq[Array[String]]) = (for {
    firstLine ← data.headOption
    isTemporal = firstLine.map { el ⇒ isDataArray(el) }
  } yield {
    isTemporal.find(_ == true).getOrElse(false)
  }).getOrElse(false)

}
