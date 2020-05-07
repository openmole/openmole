/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.plugin.task.scala

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.expansion.{ FromContext, ScalaCompilation }
import org.openmole.core.fileservice.FileService
import org.openmole.core.serializer.plugin.Plugins
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.validation._
import org.openmole.core.workspace.TmpDirectory
import org.openmole.plugin.task.external.{ External, ExternalBuilder }
import org.openmole.plugin.task.jvm._
import org.openmole.core.dsl._
import org.openmole.core.workflow.hook.FromContextHook
import org.openmole.tool.cache._

import scala.util._

object ScalaTask {

  implicit def isTask: InputOutputBuilder[ScalaTask] = InputOutputBuilder(ScalaTask.config)
  implicit def isExternal: ExternalBuilder[ScalaTask] = ExternalBuilder(ScalaTask.external)
  implicit def isInfo = InfoBuilder(info)

  implicit def isJVM: JVMLanguageBuilder[ScalaTask] = new JVMLanguageBuilder[ScalaTask] {
    override def libraries = ScalaTask.libraries
    override def plugins = ScalaTask.plugins
  }

  def apply(source: String)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {
    new ScalaTask(
      source,
      plugins = Vector.empty,
      libraries = Vector.empty,
      config = InputOutputConfig(),
      external = External(),
      info = InfoConfig()
    )
  }

  def apply(f: (Context, ⇒ Random) ⇒ Seq[Variable[_]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("ScalaTask")((ctx, rng, _) ⇒ Context(f(ctx, rng()): _*))

  def apply(f: Context ⇒ Seq[Variable[_]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) =
    ClosureTask("ScalaTask")((ctx, _, _) ⇒ Context(f(ctx): _*))

}

@Lenses case class ScalaTask(
  sourceCode: String,
  plugins:    Vector[File],
  libraries:  Vector[File],
  config:     InputOutputConfig,
  external:   External,
  info:       InfoConfig
) extends Task with ValidateTask with Plugins {

  lazy val compilation = CacheKey[ScalaCompilation.ContextClosure[java.util.Map[String, Any]]]()

  def compile(implicit newFile: TmpDirectory, fileService: FileService) = {
    implicit def m = manifest[java.util.Map[String, Any]]
    ScalaCompilation.static(
      sourceCode,
      inputs.toSeq ++ Seq(JVMLanguageTask.workDirectory),
      ScalaCompilation.WrappedOutput(outputs),
      libraries = libraries,
      plugins = plugins
    )
  }

  override def validate = {
    def libraryErrors = libraries.flatMap { l ⇒
      l.exists() match {
        case false ⇒ Some(new UserBadDataError(s"Library file $l does not exist"))
        case true  ⇒ None
      }
    }

    Validate { p ⇒
      import p._

      def compilationError =
        Try(compile) match {
          case Success(_) ⇒ Seq.empty
          case Failure(e) ⇒ Seq(e)
        }

      libraryErrors ++ compilationError
    }
  }

  override def process(taskExecutionContext: TaskExecutionContext) = {
    def processCode =
      FromContext { p ⇒
        import p._

        val scalaCompilation = taskExecutionContext.cache.getOrElseUpdate(compilation, compile)

        val map = scalaCompilation(context, p.random, p.newFile)
        outputs.toSeq.map {
          o ⇒ Variable.unsecure(o, Option(map.get(o.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
        }: Context
      }

    JVMLanguageTask.process(taskExecutionContext, libraries, external, processCode, outputs)
  }
}

