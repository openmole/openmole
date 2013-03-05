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
package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.InputEvent
import javax.swing.BorderFactory
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.action.MoveProvider
import org.netbeans.api.visual.action.RectangularSelectDecorator
import org.netbeans.api.visual.action.RectangularSelectProvider
import org.netbeans.api.visual.action.SelectProvider
import org.netbeans.api.visual.graph.GraphScene
import org.openmole.ide.core.model.commons.Constants
import org.netbeans.api.visual.model.ObjectState
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.LayerWidget
import org.netbeans.api.visual.action.ConnectorState
import org.netbeans.api.visual.widget.Scene
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.model.sampling._
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.implementation.dialog.DialogFactory
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.panel._
import org.openmole.ide.core.implementation.provider.MoleSceneMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.JavaConversions._
import org.openmole.ide.core.model.panel.PanelMode._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.swing.Panel
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.builder.SceneFactory

abstract class MoleScene(n: String = "") extends GraphScene.StringGraph with IMoleScene
    with SelectProvider
    with RectangularSelectDecorator
    with RectangularSelectProvider {
  moleScene ⇒

  val manager = new MoleSceneManager(n)
  var obUI: Option[Widget] = None
  val capsuleLayer = new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  val propertyLayer = new LayerWidget(this)
  val extraPropertyLayer = new LayerWidget(this)
  var currentSlotIndex = 1
  // val _selection = new HashSet[ICapsuleUI]

  val currentPanel = new MigPanel("")
  val currentExtraPanel = new MigPanel("")

  val moveAction = ActionFactory.createMoveAction(null, new MultiMoveProvider)
  val selectAction = ActionFactory.createSelectAction(this)

  addChild(capsuleLayer)
  addChild(connectLayer)
  addChild(propertyLayer)
  addChild(extraPropertyLayer)

  val extraPropertyWidget = new ComponentWidget(this, currentExtraPanel.peer) {
    setVisible(false)
  }
  val propertyWidget = new ComponentWidget(this, currentPanel.peer) {
    setVisible(false)
  }
  extraPropertyLayer.addChild(extraPropertyWidget)
  propertyLayer.addChild(propertyWidget)

  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))
  getActions.addAction(ActionFactory.createRectangularSelectAction(this, capsuleLayer, this))

  val connectAction = ActionFactory.createExtendedConnectAction(null,
    connectLayer,
    new MoleSceneTransitionProvider,
    InputEvent.SHIFT_MASK)

  val dataChannelAction = ActionFactory.createExtendedConnectAction(null, connectLayer,
    new MoleSceneDataChannelProvider,
    InputEvent.CTRL_MASK)

  override def paintChildren = {
    getGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    getGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    super.paintChildren
  }

  def currentPanelUI = currentPanel.contents.headOption match {
    case Some(x: BasePanel) ⇒ x.panelUI
    case _ ⇒ throw new UserBadDataError("There is no current panel.")
  }

  def displayPropertyPanel(proxy: IDataProxyUI,
                           mode: PanelMode.Value) =
    ScenesManager.currentSceneContainer match {
      case (Some(exe: ExecutionMoleSceneContainer)) ⇒
      case _ ⇒
        closePropertyPanel
        currentPanel.contents.removeAll
        proxy match {
          case x: ITaskDataProxyUI ⇒ currentPanel.contents += new TaskPanel(x, this, mode)
          case x: IPrototypeDataProxyUI ⇒ currentPanel.contents += new PrototypePanel(x, this, mode)
          case x: IEnvironmentDataProxyUI ⇒ currentPanel.contents += new EnvironmentPanel(x, this, mode)
          case x: ISamplingCompositionDataProxyUI ⇒ currentPanel.contents += new SamplingCompositionPanel(x, this, mode)
          case x: IHookDataProxyUI ⇒ currentPanel.contents += new HookPanel(x, this, mode)
          case _ ⇒
        }
        propertyWidget.setPreferredLocation(new Point(getView.getBounds().x.toInt + 20, 20))
        propertyWidget.revalidate
        propertyWidget.setVisible(true)

        currentPanel.contents.get(currentPanel.contents.size - 1) match {
          case x: BasePanel ⇒ x.nameTextField.requestFocus
          case _ ⇒
        }
        refresh
    }

  def displayExtraPropertyPanel(compositionSamplingWidget: ISamplingCompositionWidget) = {
    currentExtraPanel.contents.removeAll
    currentExtraPanel.contents.add(compositionSamplingWidget match {
      case s: ISamplingWidget ⇒ new SamplingPanel(s, this, EXTRA)
      case f: IDomainWidget ⇒ new DomainPanel(f, this, EXTRA)
    })
    extraPropertyWidget.setVisible(true)
    extraPropertyWidget.setPreferredLocation(new Point(propertyWidget.getBounds.x.toInt + currentPanel.bounds.width + 40, 20))
    refresh
  }

  def displayExtraPropertyPanel(dproxy: IDataProxyUI) = {
    currentExtraPanel.contents.removeAll
    var freeze = false
    currentExtraPanel.contents.add(dproxy match {
      case x: IPrototypeDataProxyUI ⇒
        freeze = x.generated
        new PrototypePanel(x, this, EXTRA)
    })
    if (freeze) currentExtraPanel.contents.foreach {
      _.enabled = !freeze
    }
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
        case x: BasePanel ⇒ x.baseSave
        case _ ⇒
      }
    }

  def closePropertyPanel: Unit = {
    if (currentPanel.contents.size > 0) {
      currentPanel.contents(0) match {
        case x: BasePanel ⇒
          if (!x.created) {
            if (DialogFactory.closePropertyPanelConfirmation(x))
              saveAndClose
          } else saveAndClose
        case _ ⇒
      }
    }

    def saveAndClose = {
      closeExtraPropertyPanel
      savePropertyPanel
      currentPanel.contents.removeAll
      propertyWidget.setVisible(false)
      refresh
    }
  }

  def toSceneCoordinates(p: Point) = convertLocalToScene(p)

  def graphScene = this

  def refresh = {
    manager.refreshCache
    validate
    repaint
  }

  def createConnectEdge(sourceNodeID: String, targetNodeID: String, slotIndex: Int = 1) = {
    currentSlotIndex = slotIndex
    createEdge(sourceNodeID, targetNodeID, manager.getEdgeID)
  }

  override def createEdge(sourceNodeID: String, targetNodeID: String, id: String) = {
    addEdge(id)
    setEdgeSource(id, sourceNodeID)
    setEdgeTarget(id, targetNodeID)
  }

  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String, sourceNode: String) = {
    if (findWidget(sourceNode) != null) {
      val slotAnchor = new OutputSlotAnchor(findWidget(sourceNode).asInstanceOf[ICapsuleUI])
      findWidget(edge).asInstanceOf[ConnectorWidget].setSourceAnchor(slotAnchor)
    }
  }

  override def attachEdgeTargetAnchor(edge: String, oldTargetNode: String, targetNode: String) = {
    val targetWidget =
      if (findWidget(targetNode) != null) {
        val slotAnchor = new InputSlotAnchor((findWidget(targetNode).asInstanceOf[ICapsuleUI]), currentSlotIndex)
        findWidget(edge).asInstanceOf[ConnectorWidget].setTargetAnchor(slotAnchor)
      }
  }

  override def attachNodeWidget(n: String) = {
    capsuleLayer.addChild(obUI.get)
    obUI.get
  }

  def isAimingAllowed(w: Widget, point: Point, b: Boolean) = false

  def isSelectionAllowed(w: Widget, point: Point, b: Boolean) = true

  def select(w: Widget, point: Point, change: Boolean) {
    if (w == this) ScenesManager.clearSelection
    else {
      w match {
        case widget: CapsuleUI ⇒
          if (change) {
            if (ScenesManager.selection.contains(widget)) ScenesManager.removeFromSelection(widget)
            else ScenesManager.addToSelection(widget)
          } else {
            if (!ScenesManager.selection.contains(widget)) {
              ScenesManager.clearSelection
              ScenesManager.addToSelection(widget)
            }
          }
        case _ ⇒
      }
    }
  }

  def createSelectionWidget = {
    val widget = new Widget(this)
    widget.setOpaque(false)
    widget.setBorder(BorderFactory.createLineBorder(new Color(222, 135, 135), 2))
    widget.setForeground(Color.red)
    widget
  }

  def performSelection(rectangle: Rectangle) = {
    if (rectangle.width < 0) {
      rectangle.x += rectangle.width
      rectangle.width *= -1
    }

    if (rectangle.height < 0) {
      rectangle.y += rectangle.height
      rectangle.height *= -1
    }

    var changed = false
    getNodes.foreach {
      b ⇒
        findWidget(b) match {
          case w: CapsuleUI ⇒
            val r = new Rectangle(w.getBounds)
            r.setLocation(w.getLocation)
            if (r.intersects(rectangle)) {
              if (!ScenesManager.selection.contains(w)) {
                changed = true
                ScenesManager.addToSelection(w)
              }
            } else {
              if (ScenesManager.selection.contains(w)) {
                changed = true
                ScenesManager.removeFromSelection(w)
              }
            }
          case x ⇒
        }
    }
  }

  class MultiMoveProvider extends MoveProvider {

    val originals = new HashMap[ICapsuleUI, Point]
    var original: Option[Point] = None

    def movementStarted(widget: Widget) = {
      ScenesManager.selection.foreach {
        o ⇒
          originals += o -> o.widget.getPreferredLocation
      }
    }

    def movementFinished(widget: Widget) = {
      originals.clear
      original = None
    }

    def getOriginalLocation(widget: Widget) = {
      widget match {
        case x: ICapsuleUI ⇒
          if (!ScenesManager.selection.contains(x)) {
            ScenesManager.clearSelection
            ScenesManager.addToSelection(x)
            x.repaint
          }
          original = Some(widget.getPreferredLocation)
          original.get
        case _ ⇒
          ScenesManager.clearSelection
          new Point
      }
    }

    def setNewLocation(widget: Widget, location: Point) {
      original match {
        case Some(o: Point) ⇒
          val dx = location.x - o.x
          val dy = location.y - o.y
          originals.foreach {
            case (k, v) ⇒
              k.widget.setPreferredLocation(new Point(v.x + dx, v.y + dy))
          }
        case _ ⇒
      }
    }
  }

  class MoleSceneTransitionProvider extends ConnectProvider {
    var source: Option[String] = None
    var target: Option[String] = None

    override def isSourceWidget(sourceWidget: Widget): Boolean = {
      val o = findObject(sourceWidget)
      source = None
      if (isNode(o)) source = Some(o.asInstanceOf[String])
      var res = false
      sourceWidget match {
        case x: ICapsuleUI ⇒ {
          res = source.isDefined
        }
      }
      res
    }

    override def isTargetWidget(sourceWidget: Widget, targetWidget: Widget): ConnectorState = {
      val o = findObject(targetWidget)
      target = None
      if (isNode(o)) target = Some(o.asInstanceOf[String])
      if (targetWidget.getClass.equals(classOf[InputSlotWidget])) {
        val iw = targetWidget.asInstanceOf[InputSlotWidget]
        currentSlotIndex = iw.index
        if (source.equals(target)) return ConnectorState.REJECT_AND_STOP
        else return ConnectorState.ACCEPT
      }
      if (o == null) return ConnectorState.REJECT
      return ConnectorState.REJECT_AND_STOP
    }

    override def hasCustomTargetWidgetResolver(scene: Scene): Boolean = false

    override def resolveTargetWidget(scene: Scene, sceneLocation: Point): Widget = null

    override def createConnection(sourceWidget: Widget, targetWidget: Widget) = {
      val sourceCapsuleUI = sourceWidget.asInstanceOf[CapsuleUI]
      SceneFactory.transition(moleScene,
        sourceCapsuleUI,
        targetWidget.asInstanceOf[InputSlotWidget],
        sourceCapsuleUI.dataUI.transitionType)
      CheckData.checkMole(moleScene)
    }
  }

  class MoleSceneDataChannelProvider extends MoleSceneTransitionProvider {
    override def createConnection(sourceWidget: Widget, targetWidget: Widget) = {
      SceneFactory.dataChannel(moleScene,
        sourceWidget.asInstanceOf[CapsuleUI],
        targetWidget.asInstanceOf[InputSlotWidget])
      CheckData.checkMole(moleScene)
    }
  }

  class MoleSceneSelectDecorator(scene: Scene) extends RectangularSelectDecorator {
    def createSelectionWidget = {
      val widget = new Widget(scene)
      widget.setBorder(BorderFactory.createLineBorder(new Color(255, 0, 0)))
      widget.setOpaque(true)
      widget
    }
  }

}
