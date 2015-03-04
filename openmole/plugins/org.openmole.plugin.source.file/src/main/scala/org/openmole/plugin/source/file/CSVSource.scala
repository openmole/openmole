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
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.task._
import java.io._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.plugin.tool.csv.{ CSVToVariablesBuilder, CSVToVariables }

import collection.mutable.ListBuffer
import java.math._
import collection.JavaConversions._
import reflect.ClassTag
import org.openmole.core.workflow.mole.{ SourceBuilder, Source }
import org.openmole.core.workflow.mole.ExecutionContext

object CSVSource {

  trait CSVSourceBuilder extends SourceBuilder with CSVToVariablesBuilder { builder ⇒
    override def addColumn(name: String, proto: Prototype[_]): this.type = {
      addOutput(proto.toArray)
      super.addColumn(name, proto)
    }

    override def addFileColumn(name: String, dir: File, proto: Prototype[File]): this.type = {
      addOutput(proto.toArray)
      super.addFileColumn(name, dir, proto)
    }

    trait Built <: super.Built with BuiltCSVToVariables
  }

  def apply(path: ExpandedString) = {
    val _path = path
    new CSVSourceBuilder { builder ⇒
      def toSource = new CSVSource with Built {
        val path = _path
      }
    }
  }

}

abstract class CSVSource extends Source with CSVToVariables {

  def path: ExpandedString

  override def process(context: Context, executionContext: ExecutionContext): Context = {
    val file = new File(path.from(context))
    val transposed = toVariables(file, context).toSeq.transpose

    def variables =
      for {
        v ← transposed
      } yield {
        val p = v.head.prototype
        val content =
          if (v.isEmpty) Array.empty
          else v.map(_.value).toArray(ClassTag(p.`type`.runtimeClass))

        Variable.unsecure(p.toArray, v.map(_.value).toArray(ClassTag(p.`type`.runtimeClass)))
      }

    context ++ variables
  }

}

