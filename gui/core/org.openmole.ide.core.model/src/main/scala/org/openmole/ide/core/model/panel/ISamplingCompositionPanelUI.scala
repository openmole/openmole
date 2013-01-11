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
import scala.collection.mutable.Set
import java.awt.Point

trait ISamplingCompositionPanelUI extends IPanelUI {
  def saveContent(name: String): ISamplingCompositionDataUI

  def scene: Scene

  def connections: List[(ISamplingComponent, ISamplingComponent)]

  def factors: List[IFactorProxyUI]

  def computeFactor(sourceProxy: ISamplingCompositionProxyUI): Option[IFactorProxyUI]

  def addDomain(domainProxy: IDomainProxyUI,
                location: Point,
                d: Boolean = true)

  def addSampling(samplingProxy: ISamplingProxyUI,
                  location: Point,
                  display: Boolean = true)

  def remove(factorWidget: ISamplingComponent)

  def setFinalSampling(samplingProxy: ISamplingCompositionProxyUI)

  def setSamplingProxy(samplingProxy: ISamplingCompositionProxyUI, b: Boolean): Unit

  def testConnections(arityTest: Boolean): Unit

  def dataUI: ISamplingCompositionDataUI

  def firstNoneModifierDomain(domain: IDomainDataUI): Option[IDomainDataUI]

  def firstSampling(proxy: ISamplingCompositionProxyUI): ISamplingCompositionProxyUI

  def update(domain: IDomainWidget): Unit

  def updateNext(domain: IDomainWidget): (ISamplingCompositionWidget, Option[ISamplingCompositionWidget])

  def updateNext(source: ISamplingComponent,
                 target: ISamplingComponent): (ISamplingCompositionWidget, Option[ISamplingCompositionWidget])

  def updatePrevious(source: IDomainWidget,
                     target: ISamplingCompositionWidget): Unit

  def updatePrevious(domain: IDomainWidget): Unit

  def updateConnections: Unit
}
