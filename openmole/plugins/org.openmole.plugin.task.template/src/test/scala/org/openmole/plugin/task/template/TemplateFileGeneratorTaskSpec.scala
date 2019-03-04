/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.plugin.task.template

import java.io.File

import org.openmole.core.context.{ Context, Val }
import org.openmole.plugin.task.template.TemplateData._
import org.openmole.tool.hash._
import org.scalatest._

class TemplateFileGeneratorTaskSpec extends FlatSpec with Matchers {
  implicit val plugins = PluginSet.empty

  "A template file generator task" should "parse a template file and evalutate the ${} expressions" in {
    val outputP = Val[File]("file1")
    val t1 = TemplateFileTask(templateFile, outputP)
    t1.toTask.process(Context.empty)(outputP).hash should equal(targetFile.hash)
  }

}