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

package org.openmole.ide.core.implementation.sampling

import scala.swing.Panel
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.workflow.ISceneContainer

trait ISamplingCompositionWidget extends Panel {
  def proxy: SamplingOrDomainProxyUI

  def update: Unit

  def scenePanelUI: SamplingCompositionPanelUI

  def displayOnMoleScene(proxy: SamplingOrDomainProxyUI): Unit = ScenesManager.currentSceneContainer match {
    case Some(s: ISceneContainer) ⇒
      // s.scene.displayPropertyPanel(proxy, update)
      s.scene.displaySamplingPropertyPanel(this)
    case _ ⇒
  }
}