package org.openmole.core

import au.com.bytecode.opencsv.CSVReader
import org.openmole.core.context.ValType

import scala.reflect.ClassTag

/*
 * Copyright (C) 2019 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package object csv {
  import java.io.{ FileReader, PrintStream }
  import java.math.{ BigDecimal, BigInteger }

  import org.openmole.core.context.{ Context, Val, Variable }
  import org.openmole.core.exception.UserBadDataError
  import org.openmole.core.tools.io.Prettifier._
  import org.openmole.tool.file._
  import org.openmole.tool.stream._

  import scala.annotation.tailrec

  def header(prototypes: Seq[Val[_]], values: Seq[Any], arrayOnRow: Boolean) =
    if (!arrayOnRow) prototypes.map(_.name).mkString(",")
    else
      def arrayHeaders(v: Any, h: String): Seq[String] =
        v match
          case v: Array[_] ⇒ v.zipWithIndex.flatMap { case (e, i) ⇒ arrayHeaders(e, s"${h}$$${i}") }
          case v: Seq[_]   ⇒ v.zipWithIndex.flatMap { case (e, i) ⇒ arrayHeaders(e, s"${h}$$${i}") }
          case v           ⇒ Seq(h)

      (prototypes zip values).flatMap {
        case (p, v) ⇒ arrayHeaders(v, "").map(h ⇒ s"${p.name}$h")
      }.mkString(",")

  def writeVariablesToCSV(
    file: File,
    variables: Seq[Variable[_]],
    unrollArray: Boolean = false,
    arrayOnRow: Boolean = false,
    gzip: Boolean = false,
    append: Boolean = false): Unit =
    def headerValue = header(variables.map(_.prototype), variables.map(_.value), arrayOnRow)
    file.withPrintStream(create = true, append = append, gz = gzip): ps =>
      appendVariablesToCSV(
        output = ps,
        header = Some(headerValue),
        values = variables.map(_.value),
        unrollArray = unrollArray,
        arrayOnRow = arrayOnRow
      )

  def appendVariablesToCSV(
    output:      PrintStream,
    header:      ⇒ Option[String] = None,
    values:      Seq[Any],
    unrollArray: Boolean          = false,
    arrayOnRow:  Boolean          = false,
    margin:      String           = ""): Unit = {

    header.foreach(h ⇒ output.appendLine { margin + h })

    def csvLine(v: Seq[Any]): String = {
      def format(v: Any): String =
        v match {
          case v: Array[_] ⇒ s"[${v.map(format).mkString(",")}]"
          case v: Seq[_]   ⇒ s"[${v.map(format).mkString(",")}]"
          case v           ⇒ v.prettify()
        }

      def quote(v: Any): String =
        v match {
          case v: Array[_] ⇒ s""""${format(v)}""""
          case v: Seq[_]   ⇒ s""""${format(v)}""""
          case v           ⇒ v.prettify()
        }

      v.map(quote).mkString(",")
    }

    def unroll(v: Seq[Any]) = {
      def writeLines(lists: Seq[List[Any]]): Unit = {
        output.appendLine(margin + csvLine(lists.map(_.head)))

        val lastLine = lists.forall(_.tail.isEmpty)
        if (!lastLine) {
          val skipHead = lists.map {
            case h :: Nil ⇒ h :: Nil
            case _ :: t   ⇒ t
            case Nil      ⇒ Nil
          }

          writeLines(skipHead)
        }
      }

      val lists: Seq[List[Any]] =
        v map {
          case v: Array[_] ⇒ v.toList
          case v: Seq[_]   ⇒ v.toList
          case v           ⇒ List(v)
        }

      if (lists.forall(!_.isEmpty)) writeLines(lists)
    }

    def onRow(v: Seq[Any]) = {
      def arrayValues(v: Any): Seq[Any] =
        v match {
          case v: Array[_] ⇒ v.flatMap(arrayValues)
          case v: Seq[_]   ⇒ v.flatMap(arrayValues)
          case v           ⇒ Seq(v)
        }

      output.appendLine(margin + csvLine(arrayValues(v)))
    }

    if (unrollArray) unroll(values)
    else if (arrayOnRow) onRow(values)
    else output.appendLine(margin + csvLine(values))
  }

  /**
   * Builds the plan.
   *
   */
  def csvToVariables(
    file:      File,
    columns:   Seq[(String, Val[_])],
    separator: Option[Char]          = None): Iterator[Iterable[Variable[_]]] = {
    val reader = new CSVReader(new FileReader(file), separator.getOrElse(','))
    val headers = reader.readNext.toArray

    val columnsIndexes = columns.map {
      case (name, _) ⇒
        val i = headers.indexOf(name)
        if (i == -1) throw new UserBadDataError("Unknown column name : " + name)
        else i
    }

    Iterator.continually(reader.readNext).takeWhile(_ != null).map { line ⇒
      (columns zip columnsIndexes).map {
        case ((name, v), i) ⇒ Variable.unsecure(v, matchConverter(v, line(i), name))
      }
    }
  }

  def matchConverter(v: Val[_], s: String, name: String): Any = {
    def matchArray[T: ClassTag](s: String, convert: String ⇒ T): Array[T] = {
      val trimed = s.trim
      if (!trimed.startsWith("[") || !trimed.endsWith("]")) throw new UserBadDataError(s"Array in CSV files should have the following format [.., .., ..], found $s")
      s.drop(1).dropRight(1).split(",").map { s ⇒ convert(s.trim) }
    }

    v match {
      case Val.caseDouble(v)            ⇒ s.toDouble
      case Val.caseString(v)            ⇒ s
      case Val.caseBoolean(v)           ⇒ s.toBoolean
      case Val.caseInt(v)               ⇒ s.toInt
      case Val.caseLong(v)              ⇒ s.toLong
      case Val.caseArrayDouble(v)       ⇒ matchArray(s, _.toDouble)
      case Val.caseArrayInt(v)          ⇒ matchArray(s, _.toInt)
      case Val.caseArrayLong(v)         ⇒ matchArray(s, _.toLong)
      case Val.caseArrayString(v)       ⇒ matchArray(s, identity)
      case Val.caseArrayBoolean(v)      ⇒ matchArray(s, _.toBoolean)
      case Val.caseArrayArrayDouble(v)  ⇒ matchArray(s, matchArray(_, _.toDouble))
      case Val.caseArrayArrayInt(v)     ⇒ matchArray(s, matchArray(_, _.toInt))
      case Val.caseArrayArrayLong(v)    ⇒ matchArray(s, matchArray(_, _.toLong))
      case Val.caseArrayArrayString(v)  ⇒ matchArray(s, matchArray(_, identity))
      case Val.caseArrayArrayBoolean(v) ⇒ matchArray(s, matchArray(_, _.toBoolean))
      case _                            ⇒ throw new UserBadDataError(s"Unsupported type in CSV sampling prototype $v mapped to column $name")
    }
  }

}
