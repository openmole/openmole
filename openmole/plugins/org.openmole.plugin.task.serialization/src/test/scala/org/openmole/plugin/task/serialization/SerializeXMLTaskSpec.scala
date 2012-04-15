/*
 * Copyright (C) 2011 reuillon
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

package org.openmole.plugin.task.serialization

import org.openmole.core.implementation.data._
import org.openmole.core.implementation.task._
import org.openmole.core.serializer.SerializerService
import org.openmole.misc.tools.io.BufferInputStream
import org.openmole.misc.tools.io.BufferOutputStream
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.io.File
import java.io.FileInputStream
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class SerializeTaskSpec extends FlatSpec with ShouldMatchers {
  
  implicit val plugins = PluginSet.empty
    
  "SerializeTask" should "serialize input variables to files" in {
    val p = new Prototype[Int]("p")
    val pfile = new Prototype[File]("pfile")
    
    val pVal = new Variable(p, 5)
  
    val t = SerializeXMLTask("Test")
    t.serialize += (p, pfile)
    val context = t.toTask.process(Context.empty + pVal)
      
    context.contains(pfile) should equal (true)
    
    val p2 = SerializerService.deserialize[Int](new FileInputStream(context.value(pfile).get))
    
    p2 should equal (5)
  }
}

