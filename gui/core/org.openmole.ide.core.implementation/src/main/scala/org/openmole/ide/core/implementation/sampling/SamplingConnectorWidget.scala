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

import org.openmole.ide.core.model.sampling._
import swing.{ Action, Label }
import org.openmole.ide.core.implementation.workflow.PrototypeOnConnectorWidget
import org.openmole.ide.core.implementation.workflow.PrototypeOnConnectorWidget._
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.core.model.sampling.IOrdering
import org.openmole.ide.core.model.data.{ IDomainDataUI, IFactorDataUI }
import org.openmole.ide.core.implementation.dataproxy.Proxys._
import org.openmole.ide.core.implementation.dialog.ConnectorPrototypeFilterDialog.{ OrderingDialog, FactorPrototypeDialog }
import org.openmole.ide.core.model.workflow.IConnectorViewUI
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.anchor.Anchor
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.layout.LayoutFactory
import org.netbeans.api.visual.widget._
import java.awt._
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import scala.Some

class SamplingConnectorWidget(sourceWidget: Widget,
                              targetWidget: Widget,
                              scene: SamplingCompositionPanelUI) extends ConnectionWidget(scene.scene) {
  samplingConnectorWidget ⇒

  val sourceW = sourceWidget.asInstanceOf[SamplingComponent]
  val targetW = targetWidget.asInstanceOf[SceneComponent]
  var componentWidget: Option[PrototypeOnConnectorWidget] = None
  val orderingDialog = new OrderingDialog(sourceW.component.proxy, this)
  val orderingWidget = new OrderingOnConnectorWidget(scene,
    sourceW.component.proxy,
    new LinkLabel(sourceW.component.proxy.ordering.toString,
      new Action("") {
        def apply = orderingDialog.display
      }, 2, bold = true) { preferredSize = new Dimension(150, 30) })

  addChild(orderingWidget)
  setConstraint(orderingWidget, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.8f)
  orderingWidget.setOpaque(true)
  updateOrderingWidget

  lazy val factorProxyUI = scene.factorWidgets.find {
    case (factor, connector) ⇒ connector == this
  }.map {
    _._1
  }.headOption match {
    case Some(f: IFactorProxyUI) ⇒ Some(f)
    case _ ⇒ None
  }

  setStroke(new BasicStroke(2))
  setLineColor(new Color(218, 218, 218))
  setSourceAnchor(sourceAnchor(sourceWidget))
  setTargetAnchor(targetAnchor(targetWidget))
  setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)

  def update = {
    componentWidget match {
      case Some(x: PrototypeOnConnectorWidget) ⇒ x.connectorUI = preview
      case _ ⇒
    }
    updateOrderingWidget
  }

  def preview = new IConnectorViewUI {
    val preview =
      factorProxyUI match {
        case Some(factor: IFactorProxyUI) ⇒
          factor.dataUI.prototype match {
            case Some(p: IPrototypeDataProxyUI) ⇒ p.toString
            case _ ⇒ "?"
          }
        case _ ⇒ "?"
      }
  }

  def updateOrderingWidget = {
    targetW match {
      case sc: SamplingComponent ⇒ sc.component.proxy match {
        case s: SamplingProxyUI ⇒
          s.dataUI match {
            case o: IOrdering ⇒
              orderingWidget.setVisible(true)
            case _ ⇒ orderingWidget.setVisible(false)
          }
        case _ ⇒ orderingWidget.setVisible(false)

      }
      case _ ⇒ orderingWidget.setVisible(false)
    }
    repaint
    revalidate
  }

  def buildPrototypeFilterWidget =
    factorProxyUI match {
      case Some(factor: IFactorProxyUI) ⇒
        val dialog = new FactorPrototypeDialog(samplingConnectorWidget)
        componentWidget = Some(new PrototypeOnConnectorWidget(scene,
          preview,
          new LinkLabel(factor.dataUI.prototype.toString,
            new Action("") {
              def apply = {
                dialog.display
              }
            }, 2, bold = true), darkOnLight, 35))
        addChild(componentWidget.get)
        setConstraint(componentWidget.get, LayoutFactory.ConnectionWidgetLayoutAlignment.CENTER, 0.5f)
        componentWidget.get.setOpaque(true)
        dialog.availablePrototypes
      case _ ⇒
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
