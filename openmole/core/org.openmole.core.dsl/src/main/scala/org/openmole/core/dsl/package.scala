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

import org.openmole.core.macros.{ ExtractValName, Keyword }
import ExtractValName._

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
import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.reflect.macros.blackbox.{ Context ⇒ MContext }

package dsl {

  import org.openmole.core.logging.LoggerService
  import org.openmole.core.workspace.Workspace

  trait DSLPackage <: Commands
      with Serializer
      with DataPackage
      with MolePackage
      with PuzzlePackage
      with SamplingPackage
      with TaskPackage
      with ToolsPackage
      with TransitionPackage
      with BuilderPackage
      with Classes {

    def valImpl[T: c.WeakTypeTag](c: MContext): c.Expr[Prototype[T]] = {
      import c.universe._
      val n = getValName(c)
      val wt = weakTypeTag[T].tpe
      c.Expr[Prototype[T]](q"Prototype[$wt](${n})")
    }

    implicit lazy val executionContext = ExecutionContext.local
    implicit lazy val implicits = Context.empty

    lazy val workspace = Workspace
    lazy val logger = LoggerService

    implicit def authenticationProvider = Workspace.authenticationProvider

    type File = java.io.File
    def File(s: String): File = new File(s)

    implicit def stringToFile(path: String) = File(path)

    implicit def durationInt(n: Int) = duration.DurationInt(n)

    implicit def durationLong(n: Long) = duration.DurationLong(n)

    implicit def stringToDuration(s: String): FiniteDuration = org.openmole.core.tools.service.stringToDuration(s)

    def encrypt(s: String) = Workspace.encrypt(s)

    implicit def byteIsIntegral = Numeric.ByteIsIntegral

    implicit def floatAsIfIntegral = Numeric.FloatAsIfIntegral

    implicit def doubleAsIfIntegral = Numeric.DoubleAsIfIntegral

    implicit def bigDecimalAsIfIntegral = Numeric.BigDecimalAsIfIntegral

    implicit def bigIntAsIfIntegral = Numeric.BigIntIsIntegral

  }

}

package object dsl extends DSLPackage {
  import scala.language.experimental.macros

  def Val[T]: Prototype[T] = macro valImpl[T]
  def Val[T: Manifest](name: String): Prototype[T] = Prototype(name)

}
