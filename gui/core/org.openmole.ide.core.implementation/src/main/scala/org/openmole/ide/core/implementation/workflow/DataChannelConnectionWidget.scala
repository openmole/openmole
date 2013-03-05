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

package org.openmole.ide.core.implementation.workflow

import java.awt.BasicStroke
import java.awt.Color
import org.netbeans.api.visual.action.WidgetAction
import org.netbeans.api.visual.action.WidgetAction._
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.anchor.AnchorShapeFactory
import org.netbeans.api.visual.layout.LayoutFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.openmole.ide.core.model.workflow.IDataChannelUI
import org.openmole.ide.core.implementation.dialog.ConnectorPrototypeFilterDialog
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.widget._
import scala.swing.Action
import org.openmole.ide.misc.tools.image.Images._

class DataChannelConnectionWidget(scene: IMoleScene, val dataChannelUI: IDataChannelUI) extends ConnectionWidget(scene.graphScene) { dataChannelWidget ⇒

  setLineColor(new Color(188, 188, 188))
  setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 20.0f, List(10.0f).toArray, 0.0f))

  setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)
  var labeled = false

  val componentWidget = new PrototypeOnConnectorWidget(scene.graphScene,
    dataChannelUI,
    new LinkLabel(dataChannelUI.preview.toString,
      new Action("") { def apply = edit }, 10))
  setConstraint(componentWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
  componentWidget.setOpaque(true)
  addChild(componentWidget)
  scene.refresh

  def edit: Unit = {
    ConnectorPrototypeFilterDialog.display(dataChannelWidget.dataChannelUI)
    scene.refresh
  }
}