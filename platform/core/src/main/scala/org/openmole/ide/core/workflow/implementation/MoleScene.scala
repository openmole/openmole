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
package org.openmole.ide.core.workflow.implementation

import java.awt.Dimension
import java.awt.Image
import java.awt.Point	
import org.netbeans.api.visual.anchor.AnchorShape
import org.netbeans.api.visual.graph.layout.GraphLayoutFactory
import org.netbeans.api.visual.layout.LayoutFactory
import org.netbeans.api.visual.widget.ConnectionWidget
import org.netbeans.api.visual.widget.Scene
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.action.ConnectProvider
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.anchor.PointShape
import org.netbeans.api.visual.graph.GraphScene
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.provider.MoleSceneMenuProvider
import org.openmole.ide.core.workflow.model.IMoleScene
import org.netbeans.api.visual.widget.LayerWidget
import org.netbeans.api.visual.widget.Widget
import org.openide.util.ImageUtilities
import org.openmole.ide.core.provider.DnDNewTaskProvider
import org.openmole.ide.core.workflow.model.ICapsuleUI
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget
import org.openmole.ide.core.workflow.implementation.paint.OSlotWidget
import org.netbeans.api.visual.action.ReconnectProvider
import org.openmole.ide.core.palette.ElementFactories
import org.openmole.ide.core.commons.CapsuleType._
import org.openmole.ide.core.commons.TransitionType._
import org.openmole.ide.core.workflow.implementation.paint.ISlotAnchor
import org.openmole.ide.core.workflow.implementation.paint.OSlotAnchor
import org.openmole.ide.core.provider.TransitionMenuProvider

class MoleScene extends GraphScene.StringGraph with IMoleScene{
  
  val manager = new MoleSceneManager
  val MOVE= "move"
  val CONNECT = "connect"
  val RECONNECT = "connect"
  var obUI: Option[Widget]= None
  val capsuleLayer= new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  var currentSlotIndex= 1
  
  val connectAction = ActionFactory.createExtendedConnectAction(connectLayer, new MoleSceneConnectProvider)
  val reconnectAction = ActionFactory.createReconnectAction(new MoleSceneReconnectProvider)
  val moveAction = ActionFactory.createMoveAction
    
  addChild(capsuleLayer)
  addChild(connectLayer)
  
  setPreferredSize(new Dimension((Constants.SCREEN_WIDTH * 0.8).toInt, (Constants.SCREEN_HEIGHT * 0.8).toInt));
  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))
  
  getActions.addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(this)))
  setActiveTool(CONNECT)  
  
  def createEdge(sourceNodeID:String, targetNodeID: String)= {
    val ed= manager.getEdgeID
    addEdge(ed)
    setEdgeSource(ed,sourceNodeID)
    setEdgeTarget(ed,targetNodeID)
  }
 
  override def initCapsuleAdd(w: ICapsuleUI)= obUI= Some(w.asInstanceOf[Widget])
  
  override def refresh= {validate; repaint}
  
  override def attachNodeWidget(n: String)= {
    capsuleLayer.addChild(obUI.get)
    obUI.get.createActions(CONNECT).addAction(connectAction)
    obUI.get.createActions(CONNECT).addAction(moveAction)
    obUI.get.getActions.addAction(createObjectHoverAction)
    obUI.get
  } 

  override def attachEdgeWidget(e: String)= {
    val connectionWidget = new LabeledConnectionWidget(this,manager.getTransition(e))
    connectLayer.addChild(connectionWidget);
    connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
    connectionWidget.getActions.addAction(createObjectHoverAction)
    connectionWidget.getActions.addAction(createSelectAction)
    connectionWidget.getActions.addAction(reconnectAction)
    // connectionWidget.getActions.addAction(new TransitionActions(manager.getTransition(e),connectionWidget))
    connectionWidget.getActions.addAction(ActionFactory.createPopupMenuAction(new TransitionMenuProvider(this,connectionWidget)));
    connectionWidget
  }

  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String,sourceNode: String)= {
    val cw = findWidget(edge).asInstanceOf[LabeledConnectionWidget]
    cw.setSourceAnchor(new OSlotAnchor(findWidget(sourceNode).asInstanceOf[CapsuleUI]))
  }
  
  override def attachEdgeTargetAnchor(edge: String,oldTargetNode: String,targetNode: String) = findWidget(edge).asInstanceOf[LabeledConnectionWidget].setTargetAnchor(new ISlotAnchor((findWidget(targetNode).asInstanceOf[CapsuleUI]), currentSlotIndex))
  
  
  override def setLayout= {
    val graphLayout = GraphLayoutFactory.createHierarchicalGraphLayout(this, true)
    graphLayout.layoutGraph(this)
    val sceneGraphLayout = LayoutFactory.createSceneGraphLayout(this, graphLayout)
    sceneGraphLayout.invokeLayout
  }
    
  def getImageFromTransferable(transferable: Transferable): Image= {
    println(" getImageFromTransferable ")
    try{
      transferable.getTransferData(DataFlavor.imageFlavor) match {
        case o: Image  => o
      }
    } catch {case _ => ImageUtilities.loadImage("ressources/shape1.png")}
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
      if (targetWidget.getClass.equals(classOf[ISlotWidget])){
        val iw= targetWidget.asInstanceOf[ISlotWidget]
        if (! iw.startingSlot){
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
      if (manager.registerTransition(sourceCapsuleUI, targetWidget.asInstanceOf[ISlotWidget],if(sourceCapsuleUI.capsuleType == EXPLORATION_TASK) EXPLORATION_TRANSITION else BASIC_TRANSITION,None))
        createEdge(source.get, target.get)
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
      edge = None
      if (isEdge(o)) edge = Some(o.asInstanceOf[String])
      originalNode = None
      if (edge.isDefined) originalNode= Some(getEdgeTarget(edge.get))
      originalNode.isDefined
      
    }
    
    override def isReplacementWidget(connectionWidget: ConnectionWidget, replacementWidget: Widget, reconnectingSource: Boolean): ConnectorState = {
      val o= findObject(replacementWidget)
      replacementNode= None
      if (isNode(o)) replacementNode = Some(o.asInstanceOf[String])
      replacementWidget match {
        case x: OSlotWidget=> return ConnectorState.ACCEPT
        case x: ISlotWidget=> {
            val iw= replacementWidget.asInstanceOf[ISlotWidget]
            currentSlotIndex = iw.index
            return ConnectorState.ACCEPT
          }
        case _=> return ConnectorState.REJECT_AND_STOP
      }
    }
    
    override def hasCustomReplacementWidgetResolver(scene: Scene)= false
    
    override def resolveReplacementWidget(scene: Scene,sceneLocation: Point)= null
    
    override def reconnect(connectionWidget: ConnectionWidget,replacementWidget: Widget,reconnectingSource: Boolean)= {
      
      println("reconnect  ")
      val t= manager.getTransition(edge.get)
      manager.removeTransition(edge.get)
      if (replacementWidget == null) removeEdge(edge.get)
      else if (reconnectingSource) {       
        println("reconnect else if ")
        setEdgeSource(edge.get, replacementNode.get)
        val sourceW = replacementWidget.asInstanceOf[OSlotWidget].capsule
        manager.registerTransition(edge.get,sourceW, t.target, if (sourceW.capsuleType == EXPLORATION_TASK) EXPLORATION_TRANSITION else BASIC_TRANSITION,None)
      }
      else {
        println("reconnect else ")
        val targetView= replacementWidget.asInstanceOf[ISlotWidget]
        connectionWidget.setTargetAnchor(new ISlotAnchor(targetView.capsule, currentSlotIndex))
        setEdgeTarget(edge.get, replacementNode.get)   
        manager.registerTransition(edge.get,t.source, targetView,if (targetView.capsule.capsuleType == EXPLORATION_TASK) EXPLORATION_TRANSITION else BASIC_TRANSITION,None)
      }
      repaint
    }

  }
}