/*
 * Copyright (C) 2015 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

import java.io.File

import org.openmole.core.workflow.builder.{ Builder, InputOutputBuilder, SamplingBuilder }
import org.openmole.core.workflow.data.Prototype

import scala.collection.mutable.ListBuffer

trait CSVToVariablesBuilder extends Builder { builder ⇒
  private var _columns = new ListBuffer[(String, Prototype[_])]
  private var _fileColumns = new ListBuffer[(String, File, Prototype[File])]
  private var separator: Option[Char] = Some(',')

  def columns = _columns.toList
  def fileColumns = _fileColumns.toList

  def addColumn(proto: Prototype[_]): this.type = this.addColumn(proto.name, proto)
  def addColumn(name: String, proto: Prototype[_]): this.type = {
    _columns += (name -> proto)
    this
  }

  def addFileColumn(name: String, dir: File, proto: Prototype[File]): builder.type = {
    _fileColumns += ((name, dir, proto))
    this
  }

  def addFileColumn(dir: File, proto: Prototype[File]): this.type = this.addFileColumn(proto.name, dir, proto)

  def setSeparator(s: Option[Char]) = {
    separator = s
    this
  }

  trait BuiltCSVToVariables {
    val columns = builder.columns
    val fileColumns = builder.fileColumns
    val separator = builder.separator.getOrElse(',')
  }
}
