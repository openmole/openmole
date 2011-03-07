/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.plugin.task.filemanagement

import java.io.File
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.data.Prototype
import org.openmole.core.implementation.execution.Progress
import org.openmole.misc.hashservice.HashService._
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import TemplateData._

@RunWith(classOf[JUnitRunner])
class TemplateFileGeneratorTaskSpec extends FlatSpec with ShouldMatchers {

  "A template file generator task" should "parse a template file and evalutate the ${} expressions" in {  
    val outputP = new Prototype("file1", classOf[File])
    val t1 = new TemplateFileGeneratorTask("Test TemplateFileGeneratorTask",templateFile,outputP)    
    val context= new Context
    t1.process(context, new Progress)
    
    context.value(outputP).get.hash.equals(targetFile.hash) should equal (true)
  }

}