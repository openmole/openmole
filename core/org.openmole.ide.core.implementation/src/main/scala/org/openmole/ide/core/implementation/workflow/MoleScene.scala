/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.workflow

import org.netbeans.api.visual.graph.layout.GraphLayoutFactory
import org.netbeans.api.visual.layout.LayoutFactory
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.InputEvent
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.action.ReconnectProvider
import org.netbeans.api.visual.graph.GraphScene
import org.openmole.ide.core.model.commons.Constants
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.LayerWidget
import org.netbeans.api.visual.action.ConnectorState
import org.netbeans.api.visual.widget.Scene
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.panel._
import org.openmole.ide.core.implementation.provider.MoleSceneMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.JavaConversions._
import org.openmole.ide.core.model.panel.PanelMode._
import scala.swing.Panel

abstract class MoleScene(n: String = "",
                         id: Int) extends GraphScene.StringGraph with IMoleScene { moleScene ⇒

  val manager = new MoleSceneManager(n, id)
  var obUI: Option[Widget] = None
  val capsuleLayer = new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  val propertyLayer = new LayerWidget(this)
  val extraPropertyLayer = new LayerWidget(this)
  var currentSlotIndex = 1

  val currentPanel = new MigPanel("")
  val currentExtraPanel = new MigPanel("")

  val moveAction = ActionFactory.createMoveAction

  addChild(capsuleLayer)
  addChild(connectLayer)
  addChild(propertyLayer)
  addChild(extraPropertyLayer)

  val extraPropertyWidget = new ComponentWidget(this, currentExtraPanel.peer) { setVisible(false) }
  val propertyWidget = new ComponentWidget(this, currentPanel.peer) { setVisible(false) }
  extraPropertyLayer.addChild(extraPropertyWidget)
  propertyLayer.addChild(propertyWidget)

  setActiveTool(CONNECT)

  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))

  val connectAction = ActionFactory.createExtendedConnectAction(null, connectLayer, new MoleSceneConnectProvider, InputEvent.SHIFT_MASK)
  val reconnectAction = ActionFactory.createReconnectAction(new MoleSceneReconnectProvider)

  override def paintChildren = {

    getGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    getGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

    super.paintChildren
  }

  def displayPropertyPanel(proxy: IDataProxyUI,
                           mode: PanelMode.Value) = {
    closePropertyPanel
    currentPanel.contents.removeAll
    proxy match {
      case x: ITaskDataProxyUI ⇒ currentPanel.contents += new TaskPanelUI(x, this, mode)
      case x: IPrototypeDataProxyUI ⇒ currentPanel.contents += new PrototypePanelUI(x, this, mode)
      case x: IEnvironmentDataProxyUI ⇒ currentPanel.contents += new EnvironmentPanelUI(x, this, mode)
      case x: ISamplingDataProxyUI ⇒ currentPanel.contents += new SamplingPanelUI(x, this, mode)
      case _ ⇒
    }
    propertyWidget.setPreferredLocation(new Point(getView.getBounds().x.toInt + 20, 20))
    propertyWidget.revalidate
    propertyWidget.setVisible(true)

    currentPanel.contents.get(currentPanel.contents.size - 1) match {
      case x: BasePanelUI ⇒ x.nameTextField.requestFocus
      case _ ⇒
    }
    refresh
  }

  def displayExtraPropertyPanel(dproxy: IDataProxyUI) = {
    currentExtraPanel.contents.removeAll
    var freeze = false
    currentExtraPanel.contents.add(dproxy match {
      case x: IPrototypeDataProxyUI ⇒
        freeze = x.generated
        new PrototypePanelUI(x, this, EXTRA)
      case x: ISamplingDataProxyUI ⇒ new SamplingPanelUI(x, this, EXTRA)
    })
    if (freeze) currentExtraPanel.contents.foreach { _.enabled = !freeze }
    extraPropertyWidget.setVisible(true)
    extraPropertyWidget.setPreferredLocation(new Point(propertyWidget.getBounds.x.toInt + currentPanel.bounds.width + 40, 20))
    refresh
  }

  def closeExtraPropertyPanel = {
    savePropertyPanel(currentExtraPanel)
    currentExtraPanel.contents.removeAll
    extraPropertyWidget.setVisible(false)
    refresh
  }

  def savePropertyPanel = savePropertyPanel(currentPanel)

  def savePropertyPanel(panel: Panel) =
    if (panel.contents.size > 0) {
      panel.contents(0) match {
        case x: BasePanelUI ⇒ x.baseSave
        case _ ⇒
      }
    }

  def closePropertyPanel: Unit = {
    closeExtraPropertyPanel
    savePropertyPanel
    currentPanel.contents.removeAll
    propertyWidget.setVisible(false)
    refresh
  }

  def graphScene = this

  def refresh = { validate; repaint }

  def createConnectEdge(sourceNodeID: String, targetNodeID: String) =
    createEdge(sourceNodeID, targetNodeID, manager.getEdgeID)

  def createDataChannelEdge(sourceNodeID: String, targetNodeID: String) =
    createEdge(sourceNodeID, targetNodeID, manager.getDataChannelID)

  override def createEdge(sourceNodeID: String, targetNodeID: String, id: String) = {
    addEdge(id)
    setEdgeSource(id, sourceNodeID)
    setEdgeTarget(id, targetNodeID)
  }

  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String, sourceNode: String) = {
    if (findWidget(sourceNode) != null) {
      ScenesManager.connectMode match {
        case true ⇒
          findWidget(edge).asInstanceOf[ConnectorWidget].setSourceAnchor(new OutputSlotAnchor(findWidget(sourceNode).asInstanceOf[ICapsuleUI]))
        case false ⇒
          findWidget(edge).asInstanceOf[DataChannelConnectionWidget].setSourceAnchor(new OutputDataChannelAnchor(findWidget(sourceNode).asInstanceOf[ICapsuleUI]))
      }
    }
  }

  override def attachEdgeTargetAnchor(edge: String, oldTargetNode: String, targetNode: String) = {
    val targetWidget =
      if (findWidget(targetNode) != null) {
        ScenesManager.connectMode match {
          case true ⇒
            findWidget(edge).asInstanceOf[ConnectorWidget].setTargetAnchor(new InputSlotAnchor((findWidget(targetNode).asInstanceOf[ICapsuleUI]), currentSlotIndex))
          case false ⇒
            findWidget(edge).asInstanceOf[DataChannelConnectionWidget].setTargetAnchor(new InputDataChannelAnchor(findWidget(targetNode).asInstanceOf[ICapsuleUI]))
        }
      }
  }

  override def attachNodeWidget(n: String) = {
    capsuleLayer.addChild(obUI.get)
    obUI.get
  }

  class MoleSceneConnectProvider extends ConnectProvider {
    var source: Option[String] = None
    var target: Option[String] = None

    override def isSourceWidget(sourceWidget: Widget): Boolean = {
      val o = findObject(sourceWidget)
      source = None
      if (isNode(o)) source = Some(o.asInstanceOf[String])
      var res = false
      sourceWidget match {
        case x: ICapsuleUI ⇒ { res = source.isDefined }
      }
      res
    }

    override def isTargetWidget(sourceWidget: Widget, targetWidget: Widget): ConnectorState = {
      val o = findObject(targetWidget)
      target = None
      if (isNode(o)) target = Some(o.asInstanceOf[String])
      ScenesManager.connectMode match {
        case false ⇒
          targetWidget match {
            case x: TaskComponentWidget ⇒ return ConnectorState.ACCEPT
            case _: Any ⇒ ConnectorState.REJECT
          }
        case true ⇒
          if (targetWidget.getClass.equals(classOf[InputSlotWidget])) {
            val iw = targetWidget.asInstanceOf[InputSlotWidget]
            currentSlotIndex = iw.index
            if (source.equals(target)) return ConnectorState.REJECT_AND_STOP
            else return ConnectorState.ACCEPT
          }
      }
      if (o == null) return ConnectorState.REJECT
      return ConnectorState.REJECT_AND_STOP
    }

    override def hasCustomTargetWidgetResolver(scene: Scene): Boolean = false

    override def resolveTargetWidget(scene: Scene, sceneLocation: Point): Widget = null

    override def createConnection(sourceWidget: Widget, targetWidget: Widget) = {
      val sourceCapsuleUI = sourceWidget.asInstanceOf[CapsuleUI]
      ScenesManager.connectMode match {
        case true ⇒
          if (manager.registerTransition(sourceCapsuleUI, targetWidget.asInstanceOf[InputSlotWidget], sourceCapsuleUI.dataUI.transitionType, None))
            createConnectEdge(source.get, target.get)
        case false ⇒
          if (manager.registerDataChannel(sourceCapsuleUI, targetWidget.asInstanceOf[TaskComponentWidget].capsule))
            createDataChannelEdge(source.get, target.get)
      }
      CheckData.checkMole(moleScene)
    }
  }

  class MoleSceneReconnectProvider extends ReconnectProvider {

    var edge: Option[String] = None
    var originalNode: Option[String] = None
    var replacementNode: Option[String] = None

    override def reconnectingStarted(connectionWidget: ConnectionWidget, reconnectingSource: Boolean) = {}

    override def reconnectingFinished(connectionWidget: ConnectionWidget, reconnectingSource: Boolean) = {}

    def findConnection(connectionWidget: ConnectionWidget) = {
      val o = findObject(connectionWidget)
      edge = None
      if (isEdge(o)) edge = Some(o.asInstanceOf[String])
      originalNode = edge
    }

    override def isSourceReconnectable(connectionWidget: ConnectionWidget): Boolean = {
      val o = findObject(connectionWidget)
      edge = None
      if (isEdge(o)) edge = Some(o.asInstanceOf[String])
      originalNode = None
      if (edge.isDefined) originalNode = Some(getEdgeSource(edge.get))
      originalNode.isDefined
    }

    override def isTargetReconnectable(connectionWidget: ConnectionWidget): Boolean = {
      val o = findObject(connectionWidget)

      if (isEdge(o)) {
        edge = Some(o.asInstanceOf[String])
        originalNode = Some(getEdgeTarget(edge.get))
        true
      } else false
    }

    override def isReplacementWidget(connectionWidget: ConnectionWidget, replacementWidget: Widget, reconnectingSource: Boolean): ConnectorState = {
      val o = findObject(replacementWidget)
      replacementNode = None
      if (isNode(o)) replacementNode = Some(o.asInstanceOf[String])
      replacementWidget match {
        case x: OutputSlotWidget ⇒ return ConnectorState.ACCEPT
        case x: InputSlotWidget ⇒ {
          val iw = replacementWidget.asInstanceOf[InputSlotWidget]
          currentSlotIndex = iw.index
          return ConnectorState.ACCEPT
        }
        case _ ⇒ return ConnectorState.REJECT_AND_STOP
      }
    }

    override def hasCustomReplacementWidgetResolver(scene: Scene) = false

    override def resolveReplacementWidget(scene: Scene, sceneLocation: Point) = null

    override def reconnect(connectionWidget: ConnectionWidget, replacementWidget: Widget, reconnectingSource: Boolean) = {
      val t = manager.transition(edge.get)
      manager.removeTransition(edge.get)
      if (replacementWidget == null) removeEdge(edge.get)
      else if (reconnectingSource) {
        setEdgeSource(edge.get, replacementNode.get)
        val sourceW = replacementWidget.asInstanceOf[OutputSlotWidget].capsule
        manager.registerTransition(edge.get,
          sourceW,
          t.target,
          sourceW.dataUI.transitionType,
          t.condition,
          t.filteredPrototypes)
      } else {
        val targetView = replacementWidget.asInstanceOf[InputSlotWidget]
        connectionWidget.setTargetAnchor(new InputSlotAnchor(targetView.capsule, currentSlotIndex))
        setEdgeTarget(edge.get, replacementNode.get)
        manager.registerTransition(edge.get,
          t.source,
          targetView,
          targetView.capsule.dataUI.transitionType,
          t.condition,
          t.filteredPrototypes)
      }
      repaint
    }
  }
}
