/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.hook.file

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.dsl._
import org.openmole.core.workflow.mole.{ MoleExecutionContext, _ }
import org.openmole.core.workflow.validation._
import org.openmole.tool.stream._

object AppendToFileHook {

  implicit def isIO: InputOutputBuilder[AppendToFileHook] = InputOutputBuilder(AppendToFileHook.config)

  def apply(file: FromContext[File], content: FromContext[String])(implicit name: sourcecode.Name) =
    new AppendToFileHook(
      file,
      content,
      config = InputOutputConfig()
    )

}

@Lenses case class AppendToFileHook(
  file:    FromContext[File],
  content: FromContext[String],
  config:  InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    file.validate(inputs) ++ content.validate(inputs)
  }

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    val f = file.from(context)
    f.createParentDir
    f.withLock(_.append(content.from(context)))
    context
  }

}
