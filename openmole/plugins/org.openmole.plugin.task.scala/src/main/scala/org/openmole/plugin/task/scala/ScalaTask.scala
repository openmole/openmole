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
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external.{ External, ExternalBuilder }
import org.openmole.plugin.task.jvm._

import scala.util._

object ScalaTask {

  implicit def isTask: InputOutputBuilder[ScalaTask] = InputOutputBuilder(ScalaTask.config)
  implicit def isExternal: ExternalBuilder[ScalaTask] = ExternalBuilder(ScalaTask.external)

  implicit def isJVM: JVMLanguageBuilder[ScalaTask] = new JVMLanguageBuilder[ScalaTask] {
    override def libraries = ScalaTask.libraries
    override def plugins = ScalaTask.plugins
  }

  def apply(source: String) =
    new ScalaTask(
      source,
      plugins = Vector.empty,
      libraries = Vector.empty,
      config = InputOutputConfig(),
      external = External()
    )

  def apply(f: (Context, ⇒ Random) ⇒ Seq[Variable[_]]) =
    ClosureTask("ScalaTask")((ctx, rng, _) ⇒ Context(f(ctx, rng())))

  def apply(f: Context ⇒ Seq[Variable[_]]) =
    ClosureTask("ScalaTask")((ctx, _, _) ⇒ Context(f(ctx)))

}

@Lenses case class ScalaTask(
    sourceCode: String,
    plugins:    Vector[File],
    libraries:  Vector[File],
    config:     InputOutputConfig,
    external:   External
) extends JVMLanguageTask with ValidateTask {

  @transient lazy val scalaCompilation =
    ScalaWrappedCompilation.static(
      sourceCode,
      inputs.toSeq,
      ScalaWrappedCompilation.WrappedOutput(outputs),
      libraries = libraries,
      plugins = plugins
    )(manifest[java.util.Map[String, Any]])

  override def validate =
    Try(scalaCompilation) match {
      case Success(_) ⇒ Seq.empty
      case Failure(e) ⇒ Seq(e)
    }

  override def processCode(context: Context)(implicit rng: RandomProvider) = {
    val map = scalaCompilation().from(context)(rng)
    outputs.toSeq.map {
      o ⇒ Variable.unsecure(o, Option(map.get(o.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
    }
  }
}

