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

package org.openmole.ide.core.model.panel

import org.openmole.ide.core.model.sampling._
import org.openmole.ide.core.model.data._
import org.netbeans.api.visual.widget.Scene
import java.awt.Point

trait ISamplingCompositionPanelUI extends IPanelUI {
  def saveContent(name: String): ISamplingCompositionDataUI

  def scene: Scene

  def addDomain(domainProxy: IDomainProxyUI,
                location: Point,
                d: Boolean = true)

  def addSampling(samplingProxy: ISamplingProxyUI,
                  location: Point,
                  display: Boolean = true)

  def remove(factorWidget: ISamplingComponent)

  def setFinalSampling(samplingProxy: ISamplingProxyUI)

  def setSamplingProxy(samplingProxy: ISamplingProxyUI, b: Boolean): Unit

  def testConnections: Unit
}
