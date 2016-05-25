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

import monocle.Lens
import monocle.macros.Lenses
import org.openmole.core.exception.InternalProcessingError
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.validation._
import org.openmole.plugin.task.external.ExternalTask._
import org.openmole.plugin.task.jvm._

import scala.util._

object ScalaTask {

  implicit def isBuilder = new ScalaTaskBuilder[ScalaTask] {
    override def inputs: Lens[ScalaTask, PrototypeSet] = ScalaTask.inputs
    override def defaults: Lens[ScalaTask, DefaultSet] = ScalaTask.defaults
    override def name: Lens[ScalaTask, Option[String]] = ScalaTask.name
    override def outputs: Lens[ScalaTask, PrototypeSet] = ScalaTask.outputs
    override def libraries: Lens[ScalaTask, Vector[File]] = ScalaTask.libraries
    override def plugins: Lens[ScalaTask, Vector[File]] = ScalaTask.plugins
    override def inputFiles: Lens[ScalaTask, Vector[InputFile]] = ScalaTask.inputFiles
    override def resources: Lens[ScalaTask, Vector[Resource]] = ScalaTask.resources
    override def outputFiles: Lens[ScalaTask, Vector[OutputFile]] = ScalaTask.outputFiles
    override def inputFileArrays: Lens[ScalaTask, Vector[InputFileArray]] = ScalaTask.inputFileArrays
  }

  def apply(source: String) =
    new ScalaTask(
      source,
      plugins = Vector.empty,
      libraries = Vector.empty,
      inputs = PrototypeSet.empty,
      outputs = PrototypeSet.empty,
      defaults = DefaultSet.empty,
      name = None,
      inputFiles = Vector.empty,
      inputFileArrays = Vector.empty,
      outputFiles = Vector.empty,
      resources = Vector.empty
    )

  def apply(f: (Context, ⇒ Random) ⇒ Seq[Variable[_]]) =
    ClosureTask((ctx, rng) ⇒ Context(f(ctx, rng)))

  def apply(f: Context ⇒ Seq[Variable[_]]) =
    ClosureTask((ctx, _) ⇒ Context(f(ctx)))

}

@Lenses case class ScalaTask(
    source:          String,
    plugins:         Vector[File],
    libraries:       Vector[File],
    inputs:          PrototypeSet,
    outputs:         PrototypeSet,
    defaults:        DefaultSet,
    name:            Option[String],
    inputFiles:      Vector[InputFile],
    inputFileArrays: Vector[InputFileArray],
    outputFiles:     Vector[OutputFile],
    resources:       Vector[Resource]
) extends JVMLanguageTask with ValidateTask {

  @transient lazy val scalaCompilation =
    ScalaWrappedCompilation.static(
      source,
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

