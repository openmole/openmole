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
import org.openmole.ide.core.implementation.data.{ CheckData }
import org.openmole.ide.core.implementation.panel._
import org.openmole.ide.core.implementation.provider.MoleSceneMenuProvider
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.swing.Panel
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.builder.SceneFactory
import org.openmole.ide.core.model.data.{ IExplorationTaskDataUI }
import org.openmole.ide.core.model.commons.TransitionType

abstract class MoleScene extends GraphScene.StringGraph with IMoleScene
    with SelectProvider
    with RectangularSelectDecorator
    with RectangularSelectProvider {
  moleScene ⇒

  val manager: IMoleUI
  var obUI: Option[Widget] = None
  val capsuleLayer = new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  val propertyLayer = new LayerWidget(this)
  val propertyLayer2 = new LayerWidget(this)
  val propertyLayer3 = new LayerWidget(this)
  var currentSlotIndex = 1

  def firstFree = {
    def firstFree0(i: Int): Int = {
      if (currentPanels(i).contents.size == 0 || i == 2) i
      else firstFree0(i + 1)
    }
    firstFree0(0)
  }

  val currentPanels = List(new MigPanel(""), new MigPanel(""), new MigPanel(""))

  val moveAction = ActionFactory.createMoveAction(null, new MultiMoveProvider)
  val selectAction = ActionFactory.createSelectAction(this)

  addChild(capsuleLayer)
  addChild(connectLayer)
  addChild(propertyLayer)
  addChild(propertyLayer2)
  addChild(propertyLayer3)

  val propertyWidget = List(new ComponentWidget(this, currentPanels(0).peer) {
    setVisible(false)
  },
    new ComponentWidget(this, currentPanels(1).peer) {
      setVisible(false)
    },
    new ComponentWidget(this, currentPanels(2).peer) {
      setVisible(false)
    })

  propertyLayer.addChild(propertyWidget(0))
  propertyLayer2.addChild(propertyWidget(1))
  propertyLayer3.addChild(propertyWidget(2))

  getActions.addAction(ActionFactory.createRectangularSelectAction(this, capsuleLayer, this))

  val connectAction = ActionFactory.createExtendedConnectAction(null,
    connectLayer,
    new MoleSceneTransitionProvider,
    InputEvent.SHIFT_MASK)

  val dataChannelAction = ActionFactory.createExtendedConnectAction(null, connectLayer,
    new MoleSceneDataChannelProvider,
    InputEvent.CTRL_MASK)

  def add(caps: ICapsuleUI, locationPoint: Point) = {
    assert(caps.scene == this)
    initCapsuleAdd(caps)
    manager.registerCapsuleUI(caps)
    graphScene.addNode(caps.id).setPreferredLocation(locationPoint)
    CheckData.checkMole(this)
  }

  def add(trans: ITransitionUI) = {
    manager.registerConnector(trans)
    createConnectEdge(trans.source.id, trans.target.capsule.id, trans.id, trans.target.index)
    refresh
  }

  def add(dc: IDataChannelUI) = {
    manager.registerConnector(dc)
    createConnectEdge(dc.source.id, dc.target.capsule.id, dc.id)
    refresh
  }

  def startingCapsule_=(caps: ICapsuleUI) = {
    manager.startingCapsule = Some(caps)
    refresh
  }

  override def paintChildren = {
    getGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    getGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    super.paintChildren
  }

  def currentPanel = currentPanels(0).contents.headOption match {
    case Some(x: BasePanel) ⇒ x
    case _                  ⇒ throw new UserBadDataError("There is no current panel.")
  }

  def currentPanelUI = currentPanel.panelUI

  def displayCapsuleProperty(capsuleUI: ICapsuleUI, tabIndex: Int) =
    ScenesManager.currentSceneContainer match {
      case (Some(exe: ExecutionMoleSceneContainer)) ⇒
      case _ ⇒
        closePropertyPanels
        removeAll(0)
        currentPanels(0).contents += new CapsulePanel(this, capsuleUI, 0, tabIndex)
        propertyWidget(0).setPreferredLocation(new Point(getView.getBounds().x.toInt + 20, 20))
        propertyWidget(0).revalidate
        propertyWidget(0).setVisible(true)
        refresh
    }

  def displayPropertyPanel(proxy: IDataProxyUI): IBasePanel = displayPropertyPanel(proxy, firstFree)

  def displayPropertyPanel(proxy: IDataProxyUI,
                           i: Int): IBasePanel =
    ScenesManager.currentSceneContainer match {
      case (Some(exe: ExecutionMoleSceneContainer)) ⇒ throw new UserBadDataError("No displaying in execution mode")
      case _ ⇒
        closePropertyPanel(i)
        val p = proxy match {
          case x: ITaskDataProxyUI                ⇒ new TaskPanel(x, this, i)
          case x: IPrototypeDataProxyUI           ⇒ new PrototypePanel(x, this, i)
          case x: IEnvironmentDataProxyUI         ⇒ new EnvironmentPanel(x, this, i)
          case x: ISamplingCompositionDataProxyUI ⇒ new SamplingCompositionPanel(x, this, i)
          case x: IHookDataProxyUI                ⇒ new HookPanel(x, this, i)
          case x: ISourceDataProxyUI              ⇒ new SourcePanel(x, this, i)
          case _                                  ⇒ throw new UserBadDataError("No displaying available for " + proxy)
        }
        currentPanels(i).contents += p

        locate(i)

        currentPanels(i).contents.get(currentPanels(i).contents.size - 1) match {
          case x: BasePanel ⇒ x.nameTextField.requestFocus
          case _            ⇒
        }
        refresh
        p
    }

  def locate(i: Int) = {
    propertyWidget(i).setPreferredLocation(new Point(currentPanels.take(i).foldLeft(0) { (acc, panel) ⇒ acc + panel.bounds.width } + 10 * i + 10, 20))
    propertyWidget(i).revalidate
    propertyWidget(i).setVisible(true)
  }

  def displayPropertyPanel(compositionSamplingWidget: ISamplingCompositionWidget): IBasePanel = {
    saveAndClose(1)
    val p = compositionSamplingWidget match {
      case s: ISamplingWidget ⇒ new SamplingPanel(s, this, 1)
      case f: IDomainWidget   ⇒ new DomainPanel(f, this, 1)
    }
    currentPanels(1).contents += p
    locate(1)
    refresh
    p
  }

  def displayPropertyPanel(dproxy: IDataProxyUI,
                           fromPanel: IBasePanel,
                           i: Int): IBasePanel = {
    val p = displayPropertyPanel(dproxy, i)
    fromPanel.listenTo(p)
    p
  }

  def savePropertyPanel(i: Int) = savePropertyPanel(currentPanels(i))

  def savePropertyPanel(panel: Panel) = {
    if (panel.contents.size > 0) {
      panel.contents(0) match {
        case x: BasePanel ⇒ x.baseSave
        case _            ⇒
      }
    }
  }
  def closePropertyPanels = for (x ← 0 to 2) closePropertyPanel(x)

  def closePropertyPanel = closePropertyPanel(firstFree)

  def closePropertyPanel(i: Int): Unit = {
    if (i >= 0 && i <= 2) {
      if (currentPanels(i).contents.size > 0) {
        currentPanels(i).contents(0) match {
          case x: BasePanel ⇒
            if (!x.created) {
              if (DialogFactory.closePropertyPanelConfirmation(x)) {
                saveAndClose(i)
              }
            }
            else {
              saveAndClose(i)
            }
          case _ ⇒
        }
      }
      closePropertyPanel(i + 1)
    }
  }

  def removeAll(i: Int) = {
    currentPanels(i).contents.removeAll
    propertyWidget(i).setVisible(false)
    refresh
  }

  def saveAndClose(i: Int) = {
    savePropertyPanel(i)
    removeAll(i)
  }

  def toSceneCoordinates(p: Point) = convertLocalToScene(p)

  def graphScene = this

  def refresh = {
    validate
    repaint
  }

  def createConnectEdge(sourceNodeID: String, targetNodeID: String, edgeId: String, slotIndex: Int = 1) = {
    currentSlotIndex = slotIndex
    addEdge(edgeId)
    setEdgeSource(edgeId, sourceNodeID)
    setEdgeTarget(edgeId, targetNodeID)
  }

  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String, sourceNode: String) = {
    if (findWidget(sourceNode) != null) {
      val slotAnchor = new OutputSlotAnchor(findWidget(sourceNode).asInstanceOf[ICapsuleUI])
      findWidget(edge).asInstanceOf[ConnectorWidget].setSourceAnchor(slotAnchor)
    }
  }

  override def attachEdgeTargetAnchor(edge: String, oldTargetNode: String, targetNode: String) = {
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
    w match {
      case widget: CapsuleUI ⇒
        ScenesManager.changeSelection(widget)
      case _ ⇒ ScenesManager.clearSelection
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

    ScenesManager.clearSelection
    getNodes.foreach {
      b ⇒
        findWidget(b) match {
          case w: CapsuleUI ⇒
            val r = new Rectangle(w.getBounds)
            r.setLocation(w.getLocation)
            if (r.intersects(rectangle)) ScenesManager.addToSelection(w)
          case _ ⇒
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
            ScenesManager.changeSelection(x)
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
      val transition = new TransitionUI(
        sourceCapsuleUI,
        targetWidget.asInstanceOf[InputSlotWidget],
        sourceCapsuleUI.dataUI.task match {
          case Some(y: ITaskDataProxyUI) ⇒ y.dataUI match {
            case x: IExplorationTaskDataUI ⇒ TransitionType.EXPLORATION_TRANSITION
            case _                         ⇒ TransitionType.BASIC_TRANSITION
          }
          case _ ⇒ TransitionType.BASIC_TRANSITION
        })
      moleScene.add(transition)
      CheckData.checkMole(moleScene)
    }
  }

  class MoleSceneDataChannelProvider extends MoleSceneTransitionProvider {
    override def createConnection(sourceWidget: Widget, targetWidget: Widget) = {
      val dc = new DataChannelUI(
        sourceWidget.asInstanceOf[CapsuleUI],
        targetWidget.asInstanceOf[InputSlotWidget])
      moleScene.add(dc)
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
