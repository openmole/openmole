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

package org.openmole.core.implementation.hook

import java.util.logging.Level
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IDataSet
import org.openmole.core.implementation.data.DataSet
import org.openmole.core.model.data.DataModeMask._
import org.openmole.core.model.hook.ICapsuleExecutionHook
import org.openmole.core.model.job.IMoleJob
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.tools.service.Logger
import scala.ref.WeakReference

object CapsuleExecutionHook {

  sealed trait Problem

  case class MissingInput(input: IData[_]) extends Problem {
    override def toString = "Input is missing " + input
  }
  case class WrongType(input: IData[_], found: IData[_]) extends Problem {
    override def toString = "Input has incompatible type " + found + " expected " + input
  }

}

import CapsuleExecutionHook._

abstract class CapsuleExecutionHook(moleExecutionRef: WeakReference[IMoleExecution], capsuleRef: WeakReference[ICapsule]) extends ICapsuleExecutionHook with Logger {

  def this(moleExecution: IMoleExecution, capsule: ICapsule) = this(new WeakReference(moleExecution), new WeakReference(capsule))

  def errors =
    inputs.flatMap {
      i ⇒
        capsule.outputs(i.prototype.name) match {
          case Some(o) ⇒
            if (!i.prototype.isAssignableFrom(o.prototype)) Some(WrongType(i, o)) else None
          case None ⇒
            if (!(i.mode is optional)) Some(MissingInput(i)) else None
        }
    }

  {
    val e = errors
    if (!e.isEmpty) throw new UserBadDataError("Incorrect inputs: " + e.mkString(", "))
  }

  resume

  private def moleExecution = moleExecutionRef.get.getOrElse(throw new InternalProcessingError("Reference garabage collected."))
  private def capsule = capsuleRef.get.getOrElse(throw new InternalProcessingError("Reference garabage collected."))

  override def resume = CapsuleExecutionDispatcher += (moleExecution, capsule, this)
  override def release = CapsuleExecutionDispatcher -= (moleExecution, capsule, this)

  def safeProcess(moleJob: IMoleJob) =
    try process(moleJob)
    catch { case e ⇒ logger.log(Level.SEVERE, "Error durring hook execution", e) }

  def process(moleJob: IMoleJob)

  def inputs: IDataSet
}
