/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.batch.control

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap

object StorageControl {

  val ressources = new HashMap[ServiceDescription, QualityControl] with SynchronizedMap[ServiceDescription, QualityControl]

  def register(ressource: ServiceDescription, failureControl: QualityControl) = ressources.getOrElseUpdate(ressource, failureControl)

  def qualityControl(ressource: ServiceDescription): Option[QualityControl] = ressources.get(ressource)

  def withFailureControl[A](desc: ServiceDescription, op: ⇒ A): A = withFailureControl[A](desc, op, { e: Throwable ⇒ true })

  def withFailureControl[A](desc: ServiceDescription, op: ⇒ A, isFailure: Throwable ⇒ Boolean): A = {
    val qualityControl = this.qualityControl(desc)
    QualityControl.withQualityControl(qualityControl, op, isFailure)
  }

}
