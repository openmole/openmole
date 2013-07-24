/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.builder

import java.awt.Point
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.implementation.workflow.{ DataChannelUI, TransitionUI, CapsuleUI }
import org.openmole.core.model.task.ITask
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import scala.Some
import org.openmole.ide.core.implementation.dataproxy.Proxies

object SceneFactory {

  def as[T: Manifest](x: Any): T =
    if (manifest.runtimeClass.isInstance(x)) x.asInstanceOf[T]
    else throw new UserBadDataError("The Object " + x + " can not be loaded in the GUI")

  //def prototype(p: Prototype[_]) = Proxies.instance.prototypeOrElseCreate(p)
}