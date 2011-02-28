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

package org.openmole.core.implementation.tools

import org.openmole.implementation.tools.TemplateData
import org.openmole.misc.hashservice.HashService
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.openmole.core.implementation.data.Context
import org.openmole.core.implementation.tools.VariableExpansion._
import org.scalatest.matchers.ShouldMatchers
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class VariableExpansionSpec  extends FlatSpec with ShouldMatchers {

  "A expandData" should "expand all the ${} sequence from an inputStream and return a parsed OuputStream" in {
    
    val of = File.createTempFile("expand", ".test")
    expandBufferData(new Context(),new FileInputStream(TemplateData.templateFile),new FileOutputStream(of))
    HashService.computeHash(of).equals(HashService.computeHash(TemplateData.targetFile)) should equal (true) 
  }
}