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

package org.openmole.core.console

import java.io.{ PrintStream, PrintWriter, Writer }
import java.net.URLClassLoader

import org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader
import org.openmole.core.pluginmanager.PluginManager

import scala.reflect.internal.util.{ NoPosition, Position }
import scala.tools.nsc.interpreter._
import scala.tools.nsc._
import scala.tools.nsc.reporters._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class ScalaREPL(storeErrors: Boolean = false, priorityClasses: Seq[Class[_]] = Nil, jars: Seq[JFile] = Seq.empty) extends ILoop {

  System.setProperty("jline.shutdownhook", "true")
  //System.setProperty("scala.repl.debug", "true")
  override val prompt = "OpenMOLE>"

  case class ErrorMessage(error: String, line: Int)

  var errorMessages: List[ErrorMessage] = Nil

  in = new JLineReader(new JLineCompletion(this))

  super.getClass.getMethods.find(_.getName.contains("globalFuture_$eq")).get.invoke(this, Future { true }.asInstanceOf[AnyRef])

  settings = new Settings
  settings.Yreplsync.value = true
  settings

  intp = new IMain {

    override lazy val reporter = new ReplReporter(this) {
      override def error(pos: Position, msg: String): Unit = synchronized {
        if (storeErrors) {
          val compiled = new String(pos.source.content).split("\n")
          val linesLength = compiled.take(pos.line - 1).flatten.size + (pos.line - 1)

          pos match {
            case NoPosition ⇒ errorMessages :+= ErrorMessage(msg, pos.line)
            case _ ⇒
              val offset = pos.start - linesLength
              errorMessages :+=
                ErrorMessage(
                  s"""|$msg
                      |${compiled(pos.line - 1)}
                      |${new String((0 until offset).map(_ ⇒ ' ').toArray)}^""".stripMargin, pos.line)
          }
        }
        else super.error(pos, msg)
      }
    }

    override protected def newCompiler(settings: Settings, reporter: Reporter) = {
      settings.outputDirs setSingleOutput replOutput.dir
      settings.exposeEmptyPackage.value = true
      new OSGiScalaCompiler(settings, reporter, replOutput.dir, priorityClasses, jars)
    }

    override lazy val classLoader = new scala.tools.nsc.util.AbstractFileClassLoader(
      replOutput.dir,
      new CompositeClassLoader(
        priorityClasses.map(_.getClassLoader) ++
          List(new URLClassLoader(jars.toArray.map(_.toURI.toURL))) ++
          List(classOf[OSGiScalaCompiler].getClassLoader): _*)
    )
  }

}
