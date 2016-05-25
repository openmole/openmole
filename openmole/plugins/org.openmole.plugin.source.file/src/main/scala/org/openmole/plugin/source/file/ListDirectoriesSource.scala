/*
 * Copyright (C) 11/01/2015 Jonathan Passerat-Palmbach
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

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.data._
import java.io.File

import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.ExpandedString
import org.openmole.core.dsl
import dsl._
import monocle.macros.Lenses

object ListDirectoriesSource {

  implicit def isBuilder = new SourceBuilder[ListDirectoriesSource] {
    override def name = ListDirectoriesSource.name
    override def outputs = ListDirectoriesSource.outputs
    override def inputs = ListDirectoriesSource.inputs
    override def defaults = ListDirectoriesSource.defaults
  }

  def apply(path: ExpandedString, prototype: Prototype[Array[File]], regExp: ExpandedString = ".*") =
    new ListDirectoriesSource(
      path,
      prototype,
      regExp,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None
    ) set (dsl.outputs += prototype)
}

@Lenses case class ListDirectoriesSource(
    path:      ExpandedString,
    prototype: Prototype[Array[File]],
    regExp:    ExpandedString,
    inputs:    PrototypeSet,
    outputs:   PrototypeSet,
    defaults:  DefaultSet,
    name:      Option[String]
) extends Source {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val expandedPath = new File(path.from(context))
    val expandedRegExp = regExp.from(context)
    Variable(
      prototype,
      expandedPath.listRecursive { f: File ⇒ f.isDirectory && f.getName.matches(expandedRegExp) } toArray
    )
  }
}
