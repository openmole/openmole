/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.workflow

import java.awt.Point
import org.netbeans.api.visual.graph.GraphScene
import org.openmole.ide.core.model.panel.{ IBasePanel, IPanelUI, PanelMode }
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.LayerWidget
import org.openmole.ide.core.model.sampling.ISamplingCompositionWidget
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import scala.swing.Panel

trait IMoleScene {
  def manager: IMoleSceneManager

  def refresh

  def validate

  def capsuleLayer: LayerWidget

  def initCapsuleAdd(w: ICapsuleUI)

  def graphScene: GraphScene[String, String]

  def createConnectEdge(sourceNodeID: String, targetNodeID: String, slotIndex: Int = 1)

  def createEdge(sourceNodeID: String, targetNodeID: String, id: String)

  def isBuildScene: Boolean

  def savePropertyPanel: Unit

  def savePropertyPanel(panel: Panel): Unit

  def displayCapsuleProperty(capsuleDataUI: ICapsuleUI, index: Int)

  def displayPropertyPanel(proxy: IDataProxyUI, mode: PanelMode.Value)

  def displayExtraPropertyPanel(proxy: IDataProxyUI, fromPanel: IBasePanel, mode: PanelMode.Value): Unit

  def displayExtraPropertyPanel(proxy: IDataProxyUI, mode: PanelMode.Value): IBasePanel

  def displayExtraPropertyPanel(samplingCompositionWidget: ISamplingCompositionWidget)

  def currentPanelUI: IPanelUI

  def closeExtraPropertyPanel

  def closePropertyPanel

  def propertyWidget: ComponentWidget

  def extraPropertyWidget: ComponentWidget

  def removeEdge(id: String)

  override def toString = manager.name

  def toSceneCoordinates(p: Point): Point
}
