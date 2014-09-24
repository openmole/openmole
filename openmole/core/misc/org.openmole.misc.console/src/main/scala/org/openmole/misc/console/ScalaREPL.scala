/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.misc.console

import scala.tools.nsc.interpreter._
import scala.tools.nsc._
import org.openmole.misc.osgi._
import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class ScalaREPL extends ILoop {

  System.setProperty("jline.shutdownhook", "true")
  //System.setProperty("scala.repl.debug", "true")
  override val prompt = "OpenMOLE>"

  intp = new IMain {

    override protected def newCompiler(settings: Settings, reporter: Reporter) = {
      settings.outputDirs setSingleOutput replOutput.dir
      settings.exposeEmptyPackage.value = true
      new OSGiScalaCompiler(settings, reporter, replOutput.dir)
    }

    /*override def interpret(s: String) = {
      val r = super.interpret(s)
      replOutput.dir.clear
      r
    }*/

    override lazy val classLoader =
      new AbstractFileClassLoader(replOutput.dir, classOf[OSGiScalaCompiler].getClassLoader)

  }

  in = new JLineReader(new JLineCompletion(this))

  super.getClass.getMethods.find(_.getName.contains("globalFuture_$eq")).get.invoke(this, Future { true }.asInstanceOf[AnyRef])

  settings = new Settings
  settings.Yreplsync.value = true
  settings

}
