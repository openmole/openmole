/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.core.model.data

import org.openmole.ide.core.model.dataproxy._
import org.openmole.core.model.mole._
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.commons.CapsuleType
import scala.collection.mutable.HashMap

trait ICapsuleDataUI {
  def id: Int

  def task: Option[ITaskDataProxyUI]

  def task_=(t: Option[ITaskDataProxyUI])

  def sampling: Option[ISamplingCompositionDataProxyUI]

  def sampling_=(s: Option[ISamplingCompositionDataProxyUI])

  def environment: Option[IEnvironmentDataProxyUI]

  def environment_=(e: Option[IEnvironmentDataProxyUI])

  def hooks: HashMap[Class[_ <: Hook], IHookDataUI]

  def hooks_=(hm: HashMap[Class[_ <: Hook], IHookDataUI])

  def unhookAll: Unit

  def transitionType: TransitionType.Value

  def capsuleType: CapsuleType

  def capsuleType_=(t: CapsuleType)
}
