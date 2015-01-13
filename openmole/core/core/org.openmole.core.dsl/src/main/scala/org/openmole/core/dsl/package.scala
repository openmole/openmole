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

package org.openmole.core

import java.io.File
import java.util.concurrent.TimeUnit

import scala.language.experimental.macros
import org.openmole.core.serializer._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.execution._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.transition._
import org.openmole.core.workflow.builder._
import org.openmole.misc.logging._
import org.openmole.misc.macros.ExtractValName._
import org.openmole.misc.pluginmanager._
import org.openmole.misc.workspace._
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.reflect.macros.blackbox.{ Context ⇒ MContext }

package object dsl extends Commands
    with Serializer
    with DataPackage
    with MolePackage
    with PuzzlePackage
    with SamplingPackage
    with TaskPackage
    with ToolsPackage
    with TransitionPackage
    with BuilderPackage {

  lazy val Prototype = org.openmole.core.workflow.data.Prototype

  def Val[T: Manifest](name: String) = Prototype(name)

  def Val[T]: Prototype[T] = macro valImpl[T]

  def valImpl[T: c.WeakTypeTag](c: MContext): c.Expr[Prototype[T]] = {
    import c.universe._
    val n = getValName(c)
    val wt = weakTypeTag[T].tpe
    c.Expr[Prototype[T]](q"Prototype[$wt](${n})")
  }

  implicit lazy val executionContext = ExecutionContext.local
  implicit lazy val implicits = Context.empty
  implicit def stringToFile(path: String) = new File(path)

  lazy val workspace = Workspace
  lazy val logger = LoggerService

  implicit def authenticationProvider = Workspace.authenticationProvider

  type File = java.io.File
  def File(s: String) = new File(s)

  implicit def durationInt(n: Int) = duration.DurationInt(n)
  implicit def durationLong(n: Long) = duration.DurationLong(n)
  implicit def stringToDuration(s: String): FiniteDuration = org.openmole.misc.tools.service.stringToDuration(s)

  lazy val LocalEnvironment = org.openmole.core.workflow.execution.local.LocalEnvironment

  def encrypt(s: String) = Workspace.encrypt(s)

}
