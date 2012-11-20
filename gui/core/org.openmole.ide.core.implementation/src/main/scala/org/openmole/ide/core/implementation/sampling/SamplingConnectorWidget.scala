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
import org.openmole.ide.core.implementation.workflow.PrototypeOnConnectorWidget._
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
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI

class SamplingConnectorWidget(sourceWidget: Widget,
                              targetWidget: Widget,
                              scene: ISamplingCompositionPanelUI,
                              var factor: Option[IFactorDataUI] = None) extends ConnectionWidget(scene.scene) {
  samplingConnectorWidget ⇒

  val sourceW = sourceWidget.asInstanceOf[SamplingComponent]
  val targetW = targetWidget.asInstanceOf[SamplingComponent]
  var componentWidget: Option[PrototypeOnConnectorWidget] = None

  setStroke(new BasicStroke(2))
  setLineColor(new Color(218, 218, 218))
  setSourceAnchor(sourceAnchor(sourceWidget))
  setTargetAnchor(targetAnchor(targetWidget))
  setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)

  factor match {
    case Some(f: IFactorDataUI) ⇒ buildPrototypeFilterWidget
    case _ ⇒
  }

  def updateFactor(p: IPrototypeDataProxyUI) = factor match {
    case Some(f: IFactorDataUI) ⇒
      factor = Some(f.clone(p))
      factor.get.domain.factorDataUI = factor
      componentWidget.get.connectorUI = preview(factor.get)
      repaint
      revalidate
    case _ ⇒
  }

  def preview(factor: IFactorDataUI) = new IConnectorViewUI {
    var preview = factor.prototype match {
      case Some(p: IPrototypeDataProxyUI) ⇒ p.toString
      case _ ⇒ "?"
    }
  }

  def buildPrototypeFilterWidget = {
    preview(factor.get)
    componentWidget = Some(new PrototypeOnConnectorWidget(scene.scene,
      preview(factor.get),
      new LinkLabel(factor.get.prototype.toString,
        new Action("") {
          def apply = {
            println("apply")
            (new FactorPrototypeDialog(prototypes.filter {
              p ⇒ factor.get.domain.dataUI.domainType.toString == p.dataUI.protoType.toString.split('.').last
            }.toList, samplingConnectorWidget)).display
          }
        }, 2, bold = true), darkOnLight))
    removeChildren
    addChild(componentWidget.get)
    setConstraint(componentWidget.get, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
    componentWidget.get.setOpaque(true)
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