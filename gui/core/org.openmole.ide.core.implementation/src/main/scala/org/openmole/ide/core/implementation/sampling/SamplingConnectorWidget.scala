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
package org.openmole.ide.core.implementation.sampling

import org.openmole.ide.core.model.sampling.{ ISamplingWidget, IDomainProxyUI, ISamplingCompositionWidget }
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI
import swing.{ Action, Label }
import java.awt.{ Cursor, Dimension, Color }
import swing.event.MousePressed
import org.openmole.ide.core.implementation.workflow.PrototypeOnConnectorWidget
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.core.model.data.IFactorDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys._
import org.openmole.ide.core.implementation.dialog.ConnectorPrototypeFilterDialog.FactorPrototypeDialog
import org.openmole.ide.core.model.workflow.IConnectorViewUI
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.anchor.Anchor
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.layout.LayoutFactory
import org.netbeans.api.visual.widget._
import java.awt._
import org.openmole.misc.exception.UserBadDataError

class SamplingConnectorWidget(sourceWidget: Widget,
                              targetWidget: Widget,
                              factor: IFactorDataUI,
                              scene: ISamplingCompositionPanelUI) extends ConnectionWidget(scene.scene) {

  val sourceW = sourceWidget.asInstanceOf[SamplingComponent]
  val targetW = targetWidget.asInstanceOf[SamplingComponent]

  setStroke(new BasicStroke(2))
  setLineColor(new Color(218, 218, 218))
  setSourceAnchor(sourceAnchor(sourceWidget))
  setTargetAnchor(targetAnchor(targetWidget))
  setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)

  targetW.component match {
    case t: ISamplingWidget ⇒
      val componentWidget = buildPrototypeFilterWidget
      addChild(componentWidget)
      setConstraint(componentWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
    case _ ⇒
  }

  def buildPrototypeFilterWidget =
    sourceW.component.proxy match {
      case d: IDomainProxyUI ⇒
        new PrototypeOnConnectorWidget(scene.scene,
          new IConnectorViewUI {
            def preview = factor.prototype.toString
          },
          new LinkLabel(factor.prototype.toString,
            new Action("") {
              def apply = (new FactorPrototypeDialog(prototypes.filter {
                p ⇒ d.dataUI.domainType == p.dataUI.protoType
              }.toList)).display
            }, 2))
      case _ ⇒ throw new UserBadDataError("No Factor representation is available.")
    }

  def sourceAnchor(w: Widget) = new Anchor(w) {
    override def compute(entry: Anchor.Entry) =
      new Result(w.convertLocalToScene(new Point(100, 19)), Anchor.Direction.RIGHT)
  }

  def targetAnchor(w: Widget) = new Anchor(w) {
    override def compute(entry: Anchor.Entry) =
      new Result(w.convertLocalToScene(new Point(0, 19)), Anchor.Direction.LEFT)
  }
}