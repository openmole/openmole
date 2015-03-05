/*
 * Copyright (C) 2011 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.tool.csv

import java.io.{ File, FileReader }
import java.math.{ BigDecimal, BigInteger }

import au.com.bytecode.opencsv.CSVReader
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.sampling._

import scala.util.Random

trait CSVToVariables {

  def columns: List[(String, Prototype[_])]
  def fileColumns: List[(String, File, Prototype[File])]
  def separator: Char

  /**
   * Builds the plan.
   *
   */
  def toVariables(file: File, context: Context): Iterator[Iterable[Variable[_]]] = {
    val reader = new CSVReader(new FileReader(file), separator)
    val headers = reader.readNext.toArray

    //test wether prototype names belong to header names
    val columnsIndexes = columns.map {
      case (name, _) ⇒
        val i = headers.indexOf(name)
        if (i == -1) throw new UserBadDataError("Unknown column name : " + name)
        else i
    }

    val fileColumnsIndexes =
      fileColumns.map {
        case (name, _, _) ⇒
          val i = headers.indexOf(name)
          if (i == -1) throw new UserBadDataError("Unknown column name : " + name)
          else i
      }

    Iterator.continually(reader.readNext).takeWhile(_ != null).map {
      line ⇒
        (columns zip columnsIndexes).map {
          case ((_, p), i) ⇒ Variable.unsecure(p, converter(p)(line(i)))
        } :::
          (fileColumns zip fileColumnsIndexes).map {
            case ((_, f, p), i) ⇒ Variable(p, new File(f, line(i)))
          } ::: Nil
    }

  }

  val conveters = Map[Class[_], (String ⇒ _)](
    classOf[BigInteger] -> (new BigInteger(_: String)),
    classOf[BigDecimal] -> (new BigDecimal(_: String)),
    classOf[Double] -> ((_: String).toDouble),
    classOf[String] -> ((_: String).toString),
    classOf[Boolean] -> ((_: String).toBoolean),
    classOf[Int] -> ((_: String).toInt),
    classOf[Float] -> ((_: String).toFloat),
    classOf[Long] -> ((_: String).toLong))

  def converter[T](p: Prototype[_]): String ⇒ _ =
    conveters.getOrElse(p.`type`.runtimeClass, throw new UserBadDataError("Unmanaged type for csv sampling for column binded to prototype " + p))

}