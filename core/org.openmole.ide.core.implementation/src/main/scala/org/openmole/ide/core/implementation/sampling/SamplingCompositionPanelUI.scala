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

import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.core.implementation.data._
import java.awt._
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.widget._
import java.awt.event.InputEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JScrollPane
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI
import org.netbeans.api.visual.action._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.provider._
import org.openmole.ide.core.model.sampling._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.netbeans.api.visual.anchor.PointShape
import org.netbeans.api.visual.anchor.AnchorFactory
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.netbeans.api.visual.anchor.Anchor

object SamplingCompositionPanelUI {
  val DEFAULT_COLOR = new Color(250, 250, 250)
}

import SamplingCompositionPanelUI._
class SamplingCompositionPanelUI(dataUI: ISamplingCompositionDataUI) extends Scene with ISamplingCompositionPanelUI { samplingCompositionPanelUI ⇒

  val factors = new HashMap[IFactorWidget, SamplingComponent]
  val samplings = new HashMap[ISamplingWidget, SamplingComponent]
  val connections = new HashSet[(String, String)]
  var finalSampling: Option[String] = None

  setBackground(new Color(77, 77, 77))
  val factorLayer = new LayerWidget(this)
  val samplingLayer = new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  addChild(factorLayer)
  addChild(samplingLayer)
  addChild(connectLayer)

  setPreferredBounds(new Rectangle(0, 0, 400, 200))

  val transitionId = new AtomicInteger
  val connectProvider = new SamplingConnectionProvider
  val connectAction = ActionFactory.createExtendedConnectAction(null, connectLayer,
    new SamplingConnectionProvider,
    InputEvent.SHIFT_MASK)
  getActions.addAction(ActionFactory.createPopupMenuAction(new SamplingSceneMenuProvider(this)))
  val moveAction = ActionFactory.createMoveAction

  dataUI.factors.foreach { f ⇒ addFactor(f._1, f._2, false) }
  dataUI.samplings.foreach { f ⇒ addSampling(f._1, f._2, false) }
  dataUI.connections.foreach { c ⇒
    connectProvider.createConnection(samplingComponentFromId(c._1),
      samplingComponentFromId(c._2))
  }
  finalSampling = dataUI.finalSampling
  setSamplingWidget(finalSampling, true)

  def peer = new MigPanel("") { peer.add(createView) }.peer

  //  def removeFactor(factorWidget: IFactorWidget) = factors.isDefinedAt(factorWidget) match {
  //    case true ⇒
  //      //factorLayer.removeChild(factors(factorWidget))
  //      removeNodeWithEdges(factors(factorWidget))
  //      factors -= factorWidget
  //      validate
  //    case _ ⇒
  //  }

  def addFactor(factorDataUI: IFactorDataUI,
                location: Point,
                display: Boolean = true) = {
    val fw = new FactorWidget(factorDataUI, display)
    val cw = new SamplingComponent(this, fw, location) {
      getActions.addAction(ActionFactory.createPopupMenuAction(new FactorMenuProvider(samplingCompositionPanelUI)))
      getActions.addAction(connectAction)
      getActions.addAction(moveAction)
    }
    factorLayer.addChild(cw)
    validate
    factors += fw -> cw
  }

  def addSampling(samplingDataUI: ISamplingDataUI,
                  location: Point,
                  display: Boolean = true) = {
    val sw = new SamplingWidget(samplingDataUI, display)
    val cw = new SamplingComponent(this, sw, location) {
      getActions.addAction(ActionFactory.createPopupMenuAction(new SamplingMenuProvider(samplingCompositionPanelUI)))
      getActions.addAction(connectAction)
      getActions.addAction(moveAction)
    }
    samplingLayer.addChild(cw)
    validate
    samplings += sw -> cw
  }

  def remove(samplingComponent: ISamplingComponent) = {
    samplingComponent.component match {
      case (s: ISamplingWidget) ⇒
        samplings.isDefinedAt(s) match {
          case true ⇒
            samplingLayer.removeChild(samplings(s))
            samplings -= s
          case _ ⇒
        }
      case (f: IFactorWidget) ⇒ factors.isDefinedAt(f) match {
        case true ⇒
          factorLayer.removeChild(factors(f))
          factors -= f
        case _ ⇒
      }
      case _ ⇒
    }
    samplingComponent.connections.foreach { cw ⇒ connectLayer.removeChild(cw) }
    samplingComponent.connections.clear
    connections.filter { c ⇒
      (c._1 == samplingComponent.component.id) || (c._2 == samplingComponent.component.id)
    }.foreach { e ⇒ connections -= e }
    revalidate
    repaint
  }

  def scene = this

  def setFinalSampling(fsampling: ISamplingWidget) = {
    setSamplingWidget(finalSampling, false)
    setSamplingWidget(fsampling, true)
    finalSampling = Some(fsampling.id)
    revalidate
    repaint
  }

  def setSamplingWidget(samplingWidget: ISamplingWidget,
                        b: Boolean): Unit = samplingWidget.isFinalSamplingWidget = b

  def setSamplingWidget(id: Option[String], b: Boolean): Unit = id match {
    case Some(i: String) ⇒ samplings.keys.find { _.dataUI.id == i } match {
      case Some(x: ISamplingWidget) ⇒ setSamplingWidget(x, b)
      case _ ⇒
    }
    case _ ⇒
  }

  def saveContent(name: String) = {
    new SamplingCompositionDataUI(name,
      factors.map { f ⇒ f._1.dataUI -> f._2.getPreferredLocation }.toList,
      samplings.map { s ⇒ s._1.dataUI -> s._2.getPreferredLocation }.toList,
      connections.toList,
      finalSampling)
  }

  def factorsAndSamplings = factors ++ samplings

  def samplingComponentFromId(id: String) = factorsAndSamplings.keys.find { _.id == id } match {
    case Some(x: ISamplingCompositionWidget) ⇒ factorsAndSamplings(x)
    case _ ⇒ throw new UserBadDataError("The sampling composition element " + id + " can not be found")
  }

  def idFromSamplingComponent(sc: SamplingComponent) = sc.component match {
    case f: IFactorWidget ⇒ f.dataUI.id
    case s: ISamplingWidget ⇒ s.dataUI.id
    case _ ⇒ throw new UserBadDataError("The sampling widget element " + sc + " can not be found")
  }

  class SamplingConnectionProvider extends ConnectProvider {
    var source: Option[ISamplingCompositionWidget] = None

    override def isSourceWidget(sourceWidget: Widget): Boolean =
      sourceWidget match {
        case x: SamplingComponent ⇒
          if (source.isDefined) source.get.color = DEFAULT_COLOR
          source = Some(x.component)
          true
        case _ ⇒
          source = None
          false
      }

    def boolToConnector(scw: ISamplingCompositionWidget,
                        b: Boolean) = {
      if (b) ConnectorState.ACCEPT
      else ConnectorState.REJECT_AND_STOP
    }

    override def isTargetWidget(sourceWidget: Widget,
                                targetWidget: Widget): ConnectorState = {
      targetWidget match {
        case s: SamplingComponent ⇒
          s.component match {
            case f: IFactorWidget ⇒ boolToConnector(f, false)
            case s: ISamplingWidget ⇒ source match {
              case Some(sw: ISamplingWidget) ⇒ boolToConnector(s, s.dataUI.isAcceptable(sw.dataUI))
              case Some(fw: IFactorWidget) ⇒ boolToConnector(s, s.dataUI.isAcceptable(fw.dataUI))
              case _ ⇒ ConnectorState.REJECT_AND_STOP
            }
            case _ ⇒
              boolToConnector(s.component, false)
              ConnectorState.REJECT_AND_STOP
          }
        case _ ⇒ ConnectorState.REJECT_AND_STOP
      }
    }

    override def hasCustomTargetWidgetResolver(scene: Scene): Boolean = false

    override def resolveTargetWidget(scene: Scene, sceneLocation: Point): Widget = null

    override def createConnection(sourceWidget: Widget, targetWidget: Widget) = {
      val connection = new ConnectionWidget(samplingCompositionPanelUI.scene)
      connection.setStroke(new BasicStroke(2))
      connection.setLineColor(new Color(218, 218, 218))
      connection.setSourceAnchor(sourceAnchor(sourceWidget))
      connection.setTargetAnchor(targetAnchor(targetWidget))
      connection.setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)
      connectLayer.addChild(connection)
      val sourceW = sourceWidget.asInstanceOf[SamplingComponent]
      val targetW = targetWidget.asInstanceOf[SamplingComponent]
      sourceW.connections += connection
      targetW.connections += connection
      connections += idFromSamplingComponent(sourceW) ->
        idFromSamplingComponent(targetW)
      source = None
    }

    def sourceAnchor(w: Widget) = new Anchor(w) {
      override def compute(entry: Anchor.Entry) =
        new Result(w.convertLocalToScene(new Point(130, 19)), Anchor.Direction.RIGHT)
    }

    def targetAnchor(w: Widget) = new Anchor(w) {
      override def compute(entry: Anchor.Entry) =
        new Result(w.convertLocalToScene(new Point(0, 19)), Anchor.Direction.LEFT)
    }
  }
}
