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

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import org.netbeans.api.visual.graph.layout.GraphLayoutFactory
import org.netbeans.api.visual.layout.LayoutFactory
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.action.ReconnectProvider
import org.netbeans.api.visual.action.SelectProvider
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
import org.openmole.ide.core.implementation.control.TopComponentsManager
import org.openmole.ide.core.implementation.panel._
import org.openmole.ide.core.implementation.provider.DnDNewTaskProvider
import org.openmole.ide.core.implementation.provider.MoleSceneMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.commons.CapsuleType._
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.widget.PropertyPanel
import scala.collection.JavaConversions._
import org.openmole.ide.core.model.panel.PanelMode._
import scala.swing.Component
import scala.swing.ScrollPane
 

abstract class MoleScene extends GraphScene.StringGraph with IMoleScene{
  
  val manager = new MoleSceneManager
  var obUI: Option[Widget]= None
  val capsuleLayer= new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  val propertyLayer= new LayerWidget(this)
  val extraPropertyLayer= new LayerWidget(this)
  var currentSlotIndex= 1
  var currentPanel : Option[BasePanelUI] = None
  
  val moveAction = ActionFactory.createMoveAction
    
  addChild(capsuleLayer)
  addChild(connectLayer)
  addChild(propertyLayer)
  addChild(extraPropertyLayer)
  
  val extraPropertyWidget = new ComponentWidget(this,new PropertyPanel(Color.WHITE,""){visible = false}.peer) 
  val propertyWidget = new ComponentWidget(this,new PropertyPanel(Color.WHITE,""){visible = false}.peer) 
  extraPropertyLayer.addChild(extraPropertyWidget)
  propertyLayer.addChild(propertyWidget)
  
  setPreferredSize(new Dimension((Constants.SCREEN_WIDTH * 0.8).toInt, (Constants.SCREEN_HEIGHT * 0.8).toInt))
  setActiveTool(CONNECT)  
  
  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))
  getActions.addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(this)))
  
  val selectAction = ActionFactory.createSelectAction(new ObjectSelectProvider)
  val connectAction = ActionFactory.createExtendedConnectAction(connectLayer, new MoleSceneConnectProvider)
  val reconnectAction = ActionFactory.createReconnectAction(new MoleSceneReconnectProvider)
  
  def displayPropertyPanel(proxy: IDataProxyUI,
                           mode: PanelMode.Value) = {
    removePropertyPanel
    closeExtraProperty
    proxy match {
      case x: ITaskDataProxyUI=> currentPanel = Some(new TaskPanelUI(x,this,mode))
      case x: IPrototypeDataProxyUI=> currentPanel = Some(new PrototypePanelUI(x,this,mode))
      case x: IEnvironmentDataProxyUI=> currentPanel = Some(new EnvironmentPanelUI(x,this,mode))
      case x: ISamplingDataProxyUI=> currentPanel = Some(new SamplingPanelUI(x,this,mode))
      case _=>
    }
    currentPanel match {
      case Some(x:BasePanelUI)=> 
        propertyWidget.addChild(new ComponentWidget(this,x.peer))
      //  propertyWidget.setPreferredSize(new Dimension(800,500))
      //  propertyWidget.setPreferredBounds(new Rectangle(0,0,800,500))
      case _=>
    }
    
    propertyWidget.setPreferredLocation(new Point(getView.getBounds().x.toInt - currentPanel.get.size.width, 20))
    getSceneAnimator.animatePreferredLocation(propertyWidget, new Point(getView.getBounds().x.toInt + currentPanel.get.bounds.width +20, 20))
    refresh
  } 
  
  def closeExtraProperty = {
    if (extraPropertyWidget.getChildren.size == 1) extraPropertyWidget.removeChildren
    refresh
  }
    
  def displayExtraProperty(dproxy: IDataProxyUI) = {
    extraPropertyWidget.removeChildren
    extraPropertyWidget.addChild(new ComponentWidget(this,dproxy match {
          case x: IPrototypeDataProxyUI=> new PrototypePanelUI(x,this,EDIT).peer
          case x: ISamplingDataProxyUI=> new SamplingPanelUI(x,this,EDIT).peer
        }))
    extraPropertyWidget.setPreferredLocation(propertyWidget.getLocation)
    getSceneAnimator.animatePreferredLocation(extraPropertyWidget, new Point(propertyWidget.getBounds.x.toInt + currentPanel.get.bounds.width +40, 20))
    refresh
  }
  
  def removePropertyPanel : Unit = {
    currentPanel match {
      case Some(x:BasePanelUI)=> x.baseSave
      case _=>
    }
    closeExtraProperty
    propertyWidget.removeChildren
    refresh
  }
    
  def graphScene = this
  
  def refresh= {validate; repaint}
  
  def setLayout= {
    val graphLayout = GraphLayoutFactory.createHierarchicalGraphLayout(this, true)
    graphLayout.layoutGraph(this)
    val sceneGraphLayout = LayoutFactory.createSceneGraphLayout(this, graphLayout)
    sceneGraphLayout.invokeLayout
  }
    
  def createConnectEdge(sourceNodeID:String, targetNodeID: String) = 
    createEdge(sourceNodeID,targetNodeID,manager.getEdgeID)
  
  def createDataChannelEdge(sourceNodeID:String, targetNodeID: String) = 
    createEdge(sourceNodeID,targetNodeID,manager.getDataChannelID)
  
  override def createEdge(sourceNodeID:String, targetNodeID: String, id: String)= {
    // val ed= manager.getEdgeID
    addEdge(id)
    setEdgeSource(id,sourceNodeID)
    setEdgeTarget(id,targetNodeID)
  }

  
  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String,sourceNode: String)= {
    if (findWidget(sourceNode) != null) {
      TopComponentsManager.connectMode match {
        case true => 
          findWidget(edge).asInstanceOf[ConnectorWidget].setSourceAnchor(new OutputSlotAnchor(findWidget(sourceNode).asInstanceOf[ICapsuleUI]))
        case false=> 
          findWidget(edge).asInstanceOf[DataChannelConnectionWidget].setSourceAnchor(new OutputDataChannelAnchor(findWidget(sourceNode).asInstanceOf[ICapsuleUI]))
      }
    }
  }
  
  override def attachEdgeTargetAnchor(edge: String,oldTargetNode: String,targetNode: String) = {
    if (findWidget(targetNode)!=null){TopComponentsManager.connectMode match {
        case true => 
          findWidget(edge).asInstanceOf[ConnectorWidget].setTargetAnchor(new InputSlotAnchor((findWidget(targetNode).asInstanceOf[ICapsuleUI]), currentSlotIndex))
        case false=> 
          findWidget(edge).asInstanceOf[DataChannelConnectionWidget].setTargetAnchor(new InputDataChannelAnchor(findWidget(targetNode).asInstanceOf[ICapsuleUI]))
      }
    }
  }
  
  override def attachNodeWidget(n: String)= {
    capsuleLayer.addChild(obUI.get)
    obUI.get
  } 

  
  class MoleSceneConnectProvider extends ConnectProvider {
    var source: Option[String]= None
    var target: Option[String]= None
    
    override def isSourceWidget(sourceWidget: Widget): Boolean= {
      val o= findObject(sourceWidget)
      source= None
      if (isNode(o)) source= Some(o.asInstanceOf[String])        
      var res= false
      sourceWidget match {
        case x: CapsuleUI=> {res = source.isDefined}
      }
      res
    }
    
    override def isTargetWidget(sourceWidget: Widget, targetWidget: Widget): ConnectorState = {
      val o= findObject(targetWidget)
      target= None
      if(isNode(o)) target= Some(o.asInstanceOf[String])
      TopComponentsManager.connectMode match {
        case false => if (targetWidget.getClass.equals(classOf[ConnectableWidget])) return ConnectorState.ACCEPT
        case true=> 
          if (targetWidget.getClass.equals(classOf[InputSlotWidget])){
            val iw= targetWidget.asInstanceOf[InputSlotWidget]
            currentSlotIndex = iw.index
            if (source.equals(target)) return ConnectorState.REJECT_AND_STOP 
            else return ConnectorState.ACCEPT
          }
      }
      if (o == null) return ConnectorState.REJECT
      return ConnectorState.REJECT_AND_STOP
    }

    override def hasCustomTargetWidgetResolver(scene: Scene): Boolean= false
    
    override def resolveTargetWidget(scene: Scene, sceneLocation: Point): Widget= null
  
    override def createConnection(sourceWidget: Widget, targetWidget: Widget)= {
      val sourceCapsuleUI = sourceWidget.asInstanceOf[CapsuleUI]
      TopComponentsManager.connectMode match {
        case true=>
          if (manager.registerTransition(sourceCapsuleUI, targetWidget.asInstanceOf[InputSlotWidget],if(sourceCapsuleUI.capsuleType == EXPLORATION_TASK) EXPLORATION_TRANSITION else BASIC_TRANSITION,None))
            createConnectEdge(source.get, target.get)
        case false=> 
          if (manager.registerDataChannel(sourceCapsuleUI, targetWidget.asInstanceOf[ConnectableWidget].capsule))
            createDataChannelEdge(source.get, target.get  )
      }
    }
  }
  
  class MoleSceneReconnectProvider extends ReconnectProvider {

    var edge: Option[String]= None
    var originalNode: Option[String]= None
    var replacementNode: Option[String]= None
    
    override def reconnectingStarted(connectionWidget: ConnectionWidget,reconnectingSource: Boolean)= {}
    
    override def reconnectingFinished(connectionWidget: ConnectionWidget,reconnectingSource: Boolean)= {}
    
    def findConnection(connectionWidget: ConnectionWidget)= {
      val o= findObject(connectionWidget)
      edge= None
      if (isEdge(o)) edge= Some(o.asInstanceOf[String])
      originalNode= edge
    }
    
    override def isSourceReconnectable(connectionWidget: ConnectionWidget): Boolean = {
      val o= findObject(connectionWidget)
      edge= None
      if (isEdge(o)) edge= Some(o.asInstanceOf[String])
      originalNode = None
      if (edge.isDefined) originalNode= Some(getEdgeSource(edge.get))
      originalNode.isDefined
    }
    
    override def isTargetReconnectable(connectionWidget: ConnectionWidget): Boolean = {
      val o = findObject(connectionWidget)
      
      if (isEdge(o)) {
        edge = Some(o.asInstanceOf[String])
        originalNode = Some(getEdgeTarget(edge.get))
        true
      }
      else false
    }
    
    override def isReplacementWidget(connectionWidget: ConnectionWidget, replacementWidget: Widget, reconnectingSource: Boolean): ConnectorState = {
      val o= findObject(replacementWidget)
      replacementNode= None
      if (isNode(o)) replacementNode = Some(o.asInstanceOf[String])
      replacementWidget match {
        case x: OutputSlotWidget=> return ConnectorState.ACCEPT
        case x: InputSlotWidget=> {
            val iw= replacementWidget.asInstanceOf[InputSlotWidget]
            currentSlotIndex = iw.index
            return ConnectorState.ACCEPT
          }
        case _=> return ConnectorState.REJECT_AND_STOP
      }
    }
    
    override def hasCustomReplacementWidgetResolver(scene: Scene)= false
    
    override def resolveReplacementWidget(scene: Scene,sceneLocation: Point)= null
    
    
    override def reconnect(connectionWidget: ConnectionWidget,replacementWidget: Widget,reconnectingSource: Boolean)= {
      val t= manager.transition(edge.get)
      manager.removeTransition(edge.get)
      if (replacementWidget == null) removeEdge(edge.get)
      else if (reconnectingSource) { 
        setEdgeSource(edge.get, replacementNode.get)
        val sourceW = replacementWidget.asInstanceOf[OutputSlotWidget].capsule
        manager.registerTransition(edge.get,sourceW, t.target, if (sourceW.capsuleType == EXPLORATION_TASK) EXPLORATION_TRANSITION else BASIC_TRANSITION,None)
      }
      else {
        val targetView= replacementWidget.asInstanceOf[InputSlotWidget]
        connectionWidget.setTargetAnchor(new InputSlotAnchor(targetView.capsule, currentSlotIndex))
        setEdgeTarget(edge.get, replacementNode.get)   
        manager.registerTransition(edge.get,t.source, targetView,if (targetView.capsule.capsuleType == EXPLORATION_TASK) EXPLORATION_TRANSITION else BASIC_TRANSITION,None)
      }
      repaint
    }
  }
  
  class ObjectSelectProvider extends SelectProvider {
        
    override def isAimingAllowed(w: Widget,localLocation: Point,invertSelection: Boolean) = false
                
    override def isSelectionAllowed(w: Widget,localLocation: Point,invertSelection: Boolean) = true
        
    override def select(w: Widget,localLocation: Point,invertSelection: Boolean) = {
      w match {
        case x: ICapsuleUI=> { 
            if(x.dataProxy.isDefined)
              x.dataProxy.get match{
                case y: ITaskDataProxyUI=> displayPropertyPanel(y,EDIT)
                case _=>
              }
          }
      }
    }
  }
  
  class PropertyWidget (scene: IMoleScene,wi: Component) extends ComponentWidget(scene.graphScene,wi.peer){
    setPreferredBounds(new Rectangle(0,0,wi.preferredSize.width,wi.preferredSize.height))
    override def paintBackground = {
      val g = scene.graphScene.getGraphics
      g.fill(wi.bounds)
      revalidate
    }
  }
}
