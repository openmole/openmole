/**
 * Created by Romain Reuillon on 22/04/16.
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
 *
 */
package org.openmole.core.workflow.validation

import org.openmole.core.context.Val
import org.openmole.core.argument.Validate
import org.openmole.core.fileservice.FileService
import org.openmole.core.tools.io.Prettifier
import org.openmole.core.workflow.hook.Hook
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.transition._
import org.openmole.core.workspace.TmpDirectory

object ValidateTask:
  def validate(task: Task): Validate =
    task match
      case t: ValidateTask => t.validate
      case _ => Validate.success

trait ValidateTask:
  def validate: Validate

trait ValidateSource:
  def validate: Validate

trait ValidateHook:
  def validate: Validate

trait ValidateTransition:
  def validate: Validate

object ValidationProblem {

  case class TaskValidationProblem(task: Task, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of task $task:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

  case class SourceValidationProblem(source: Source, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of source $source:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

  case class HookValidationProblem(hook: Hook, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of hook $hook:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

  case class TransitionValidationProblem(transition: Transition, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of transition $transition:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

  case class MoleValidationProblem(mole: Mole, errors: Seq[Throwable]) extends ValidationProblem {
    override def toString = s"Errors in validation of mole:\n" + errors.map(e ⇒ Prettifier.ExceptionPretiffier(e).stackStringWithMargin).mkString("\n")
  }

}

trait ValidationProblem extends Problem