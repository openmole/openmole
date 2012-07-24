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

package org.openmole.plugin.sampling.csv

import java.io.File
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeMap
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.implementation.sampling.SamplingBuilder
import org.openmole.core.model.data._
import java.io.FileReader
import java.math.BigInteger
import java.math.BigDecimal
import org.openmole.core.implementation.data._
import org.openmole.core.model.sampling.ISampling
import au.com.bytecode.opencsv.CSVReader
import collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object CSVSampling {

  def apply(
    file: File) = new SamplingBuilder { builder ⇒
    private var _columns = new ListBuffer[(String, IPrototype[_])]
    private var _fileColumns = new ListBuffer[(String, File, IPrototype[File])]

    def columns = _columns.toList
    def fileColumns = _fileColumns.toList

    def addColumn(proto: IPrototype[_]): this.type = this.addColumn(proto.name, proto)
    def addColumn(name: String, proto: IPrototype[_]): builder.type = {
      _columns += (name -> proto)
      this
    }

    def addFileColumn(name: String, dir: File, proto: IPrototype[File]): builder.type = {
      _fileColumns += ((name, dir, proto))
      this
    }

    def addFileColumn(dir: File, proto: IPrototype[File]): this.type = this.addFileColumn(proto.name, dir, proto)

    def toSampling = new CSVSampling(file) {
      val columns = builder.columns
      val fileColumns = builder.fileColumns
    }
  }

}

abstract sealed class CSVSampling(file: File) extends Sampling {

  override def prototypes =
    columns.map { case (_, p) ⇒ p } :::
      fileColumns.map { case (_, _, p) ⇒ p } ::: Nil

  def columns: List[(String, IPrototype[_])]
  def fileColumns: List[(String, File, IPrototype[File])]

  /**
   * Builds the plan.
   *
   */
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {
    val reader = new CSVReader(new FileReader(file))
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
          case ((_, p), i) ⇒ new Variable(p.asInstanceOf[IPrototype[Any]], converter(p)(line(i)))
        } :::
          (fileColumns zip fileColumnsIndexes).map {
            case ((_, f, p), i) ⇒ new Variable(p, new File(f, line(i)))
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

  def converter[T](p: IPrototype[_]): String ⇒ _ =
    conveters.getOrElse(p.`type`.erasure, throw new UserBadDataError("Unmanaged type for csv sampling for column binded to prototype " + p))

}