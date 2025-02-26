package org.openmole.gui.client.tool.plot

import scala.util.Try

import scala.scalajs.js.RegExp

object Tools {

  implicit def stringToDouble(seq: Array[String]): Array[Double] = Try(
    seq.map {
      _.toDouble
    }
  ).toOption.getOrElse(Array(0.0))

  val arrayRegEx = new RegExp("\\[[+-?0-9].*,*\\]")

  def isDataArray(value: String) = {
    arrayRegEx.test(value)
  }

  def dataArrayIndexes(row: Array[String]) =
    row.zipWithIndex.filter { case (r, i) => isDataArray(r) }.map {
      _._2
    }

  def dataArrayFrom(row: Array[String]) = row.filter(isDataArray)

  def arrayDataRegEx(s: String) = arrayRegEx.exec(s).map {
    _.get
  }.head.tail.dropRight(1).split(',').toSeq

  def getDataArrays(dataCol: Seq[String]) = {
    dataCol.map { d =>
      arrayRegEx.exec(d).map {
        _.get
      }.head.tail.dropRight(1).split(',').toSeq
    }
  }

  def parseDouble(s: String): Option[Double] = Try { s.toDouble }.toOption
}
