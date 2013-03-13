/*
 * Copyright (C) 19/12/12 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.source.file

import au.com.bytecode.opencsv.CSVReader
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.core.model.task._
import org.openmole.core.implementation.task._
import java.io._
import collection.mutable.ListBuffer
import org.openmole.misc.exception.UserBadDataError
import java.math._
import collection.JavaConversions._
import reflect.ClassTag
import org.openmole.core.implementation.mole.{ SourceBuilder, Source }
import org.openmole.core.model.mole.ExecutionContext

object CSVSource {

  def apply(name: String, file: File) =
    new SourceBuilder { builder ⇒

      private var _columns = new ListBuffer[(String, Prototype[_])]

      def columns = _columns.toList

      def addColumn(proto: Prototype[_]): this.type = this.addColumn(proto.name, proto)
      def addColumn(name: String, proto: Prototype[_]): builder.type = {
        _columns += (name -> proto)
        addOutput(proto.toArray)
        this
      }

      def toSource = new CSVSource(name, file) with Built {
        val columns = builder.columns
      }

    }

}

abstract class CSVSource(val name: String, val file: File) extends Source {

  def columns: List[(String, Prototype[_])]

  def p = columns.map { case (_, p) ⇒ p }

  import org.openmole.core.model.data._

  override def process(context: Context, executionContext: ExecutionContext): Context = {
    val reader = new CSVReader(new FileReader(file))
    val headers = reader.readNext.toArray

    val columnsIndexes = columns.map {
      case (name, _) ⇒
        val i = headers.indexOf(name)
        if (i == -1) throw new UserBadDataError("Unknown column name : " + name)
        else i
    }

    //manual mapping
    val transposer = Iterator.continually(reader.readNext).takeWhile(_ != null).map {
      line ⇒
        (columns zip columnsIndexes).map {
          case ((_, p), i) ⇒ converter(p)(line(i))
        }
    }.toList.transpose

    //val pToArray = columns.map { case(name,p) =>  }
    val array: Array[(String, List[_])] = headers zip transposer

    val result = (columns zip array).map {
      case ((pname, p), (name, v)) ⇒
        Variable(p.toArray.asInstanceOf[Prototype[Array[Any]]], v.toArray(ClassTag(p.`type`.runtimeClass)))
    }.toIterable

    context ++ result
  }

  val converters = Map[Class[_], (String ⇒ _)](
    classOf[BigInteger] -> (new BigInteger(_: String)),
    classOf[BigDecimal] -> (new BigDecimal(_: String)),
    classOf[Double] -> ((_: String).toDouble),
    classOf[String] -> ((_: String).toString),
    classOf[Boolean] -> ((_: String).toBoolean),
    classOf[Int] -> ((_: String).toInt),
    classOf[Float] -> ((_: String).toFloat),
    classOf[Long] -> ((_: String).toLong))

  def converter[T](p: Prototype[_]): String ⇒ _ =
    converters.getOrElse(p.`type`.runtimeClass, throw new UserBadDataError("Unmanaged type for csv sampling for column binded to prototype " + p))

}

