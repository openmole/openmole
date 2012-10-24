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

package org.openmole.core.batch.jobservice

import org.openmole.core.batch.control._
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import org.openmole.misc.workspace._
import org.openmole.core.batch.environment.BatchEnvironment

object JobServiceControl {
  val ressources = new HashMap[String, JobServiceQualityControl] with SynchronizedMap[String, JobServiceQualityControl]

  def apply(js: JobService) = ressources.getOrElseUpdate(js.id, new JobServiceQualityControl(Workspace.preferenceAsInt(BatchEnvironment.QualityHysteresis)))

  def withQualityControl[A](js: JobService)(op: ⇒ A) = {
    val qc = apply(js)
    QualityControl.timed(qc, QualityControl.withFailureControl(qc)(op))
  }
}
