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

import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.tools._
import org.openmole.plugin.tool.csv.CSVToVariables

object CSVSampling {
  def apply(file: File) = new CSVSamplingBuilder(file)
  def apply(directory: File, name: ExpandedString) = new CSVSamplingBuilder(FileList(directory, name))
}

abstract class CSVSampling(val file: FromContext[File]) extends Sampling with CSVToVariables {

  override def prototypes =
    columns.map { case (_, p) ⇒ p } :::
      fileColumns.map { case (_, _, p) ⇒ p } ::: Nil

  override def apply() = FromContext { (context, rng) ⇒ toVariables(file.from(context)(rng), context) }

}