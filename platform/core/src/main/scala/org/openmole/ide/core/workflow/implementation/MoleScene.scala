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
import org.openmole.ide.core.workflow.model.ICapsuleView
import org.netbeans.api.visual.action.ConnectorState
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.workflow.implementation.paint.LabeledConnectionWidget
import org.openmole.ide.core.workflow.implementation.paint.OSlotWidget
import org.netbeans.api.visual.action.ReconnectProvider
import org.openmole.ide.core.workflow.implementation.paint.ISlotAnchor
import org.openmole.ide.core.workflow.implementation.paint.OSlotAnchor
import org.openmole.ide.core.workflow.action.TransitionActions
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
    println("createEdge ")
    val ed= manager.getEdgeID
    addEdge(ed)
    setEdgeSource(ed,sourceNodeID)
    setEdgeTarget(ed,targetNodeID)
  }
 
  override def initCapsuleAdd(w: ICapsuleView)= obUI= Some(w.asInstanceOf[Widget])
  
  override def refresh= {validate; repaint}
  
  override def attachNodeWidget(n: String)= {
    capsuleLayer.addChild(obUI.get)
    obUI.get.createActions(CONNECT).addAction(connectAction)
    obUI.get.createActions(CONNECT).addAction(moveAction)
    obUI.get.getActions.addAction(createObjectHoverAction)
    obUI.get
  } 

  override def attachEdgeWidget(e: String)= {
    val connectionWidget = new LabeledConnectionWidget(this,manager.getTransition(e).condition)
    connectLayer.addChild(connectionWidget);
    connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG)
    connectionWidget.getActions.addAction(createObjectHoverAction)
    connectionWidget.getActions.addAction(createSelectAction)
    connectionWidget.getActions.addAction(reconnectAction)
    connectionWidget.getActions.addAction(new TransitionActions(manager.getTransition(e),connectionWidget))
    connectionWidget.getActions.addAction(ActionFactory.createPopupMenuAction(new TransitionMenuProvider(this,connectionWidget,e)));
    connectionWidget
  }

  override def attachEdgeSourceAnchor(edge: String, oldSourceNode: String,sourceNode: String)= {
    val cw = findWidget(edge).asInstanceOf[LabeledConnectionWidget]
    cw.setSourceAnchor(new OSlotAnchor(findWidget(sourceNode).asInstanceOf[CapsuleViewUI]))
    cw.setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED)
  }
  
  override def attachEdgeTargetAnchor(edge: String,oldTargetNode: String,targetNode: String) = findWidget(edge).asInstanceOf[LabeledConnectionWidget].setTargetAnchor(new ISlotAnchor((findWidget(targetNode).asInstanceOf[CapsuleViewUI]), currentSlotIndex))
  
  
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
        case x: CapsuleViewUI=> {res = source.isDefined}
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
      manager.registerTransition(new TransitionUI(sourceWidget.asInstanceOf[CapsuleViewUI], targetWidget.asInstanceOf[ISlotWidget]))
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
   //            Object object = findObject(replacementWidget);
//            replacementNode = isNode(object) ? (String) object : null;
//            if (replacementWidget.getClass().equals(OSlotWidget.class)) {
//                OSlotWidget ow = (OSlotWidget) replacementWidget;
//                return ConnectorState.ACCEPT;
//            } else if (replacementWidget.getClass().equals(ISlotWidget.class)) {
//                ISlotWidget iw = (ISlotWidget) replacementWidget;
//                MoleScene.this.currentSlotIndex = iw.getIndex();
//                return ConnectorState.ACCEPT;
//            }
//            return object != null ? ConnectorState.REJECT_AND_STOP : ConnectorState.REJECT;
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
        manager.registerTransition(edge.get,new TransitionUI(replacementWidget.asInstanceOf[OSlotWidget].capsule, t.target))
      }
      else {
        println("reconnect else ")
        val targetView= replacementWidget.asInstanceOf[ISlotWidget]
        connectionWidget.setTargetAnchor(new ISlotAnchor(targetView.capsuleView, currentSlotIndex))
        setEdgeTarget(edge.get, replacementNode.get)
        manager.registerTransition(edge.get,new TransitionUI(t.source, targetView))
      }
      repaint
    }

  }
}




//public class MoleScene extends GraphScene.StringGraph implements IMoleScene {
//
//    public static final String MOVE = "move";
//    public static final String CONNECT = "connect";
//    public static final String RECONNECT = "connect";
//    private MoleSceneManager manager = new MoleSceneManager();
//    private LayerWidget capsuleLayer = new LayerWidget(this);
//    private LayerWidget connectLayer = new LayerWidget(this);
//    private LayerWidget slotLayer = new LayerWidget(this);
//    private Widget obUI = null;
//    private int nbEdges = 0;
//    //private WidgetAction connectAction = ActionFactory.createConnectAction(connectLayer, new MoleSceneConnectProvider());
//    private WidgetAction connectAction = ActionFactory.createExtendedConnectAction(connectLayer, new MoleSceneConnectProvider());
//    private WidgetAction reconnectAction = ActionFactory.createReconnectAction(new MoleSceneReconnectProvider());
//    private WidgetAction moveAction = ActionFactory.createMoveAction();
//    private int currentSlotIndex = 1;
//
//
//    public MoleScene() {
//        super();
//        addChild(capsuleLayer);
//        addChild(connectLayer);
//
//        setPreferredSize(new Dimension((int) (Constants.SCREEN_WIDTH * 0.8), (int) (Constants.SCREEN_HEIGHT * 0.8)));
//        // view = createView();
//        getActions().addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)));
//        //getActions().addAction(ActionFactory.createRectangularSelectAction(this, taskLayer));
//       // getActions().addAction(ActionFactory.createRectangularSelectAction(this, capsuleLayer));
//        // getActions().addAction(ActionFactory.createZoomAction());
//        //  getActions().addAction(ActionFactory.createPanAction());
//
//        getActions().addAction(ActionFactory.createAcceptAction(new DnDNewTaskProvider(this)));
//
//        setActiveTool(CONNECT);
//    }
//
//    public LayerWidget getSlotLayer() {
//        return slotLayer;
//    }
//
//    @Override
//    public MoleSceneManager getManager() {
//        return manager;
//    }
//
//    public void createEdge(String sourceNodeID, String targetNodeID) {
//        String ed = manager.getEdgeID();
//        addEdge(ed);
//        setEdgeSource(ed, sourceNodeID);
//        setEdgeTarget(ed, targetNodeID);
//    }
//
//    @Override
//    public void initCapsuleAdd(ICapsuleView w) {
//        obUI = (Widget) w;
//    }
//
//    @Override
//    protected Widget attachNodeWidget(String n) {
//        capsuleLayer.addChild(obUI);
//        obUI.createActions(CONNECT).addAction(connectAction);
//        obUI.createActions(CONNECT).addAction(moveAction);
//        obUI.getActions().addAction(createObjectHoverAction());
//        return obUI;
//    }
//
//    @Override
//    protected Widget attachEdgeWidget(String e) {
//        LabeledConnectionWidget connectionWidget = new LabeledConnectionWidget(this,manager.getTransition(e).getCondition());
////        connectionWidget.setPaintControlPoints (true);
////        connectionWidget.setControlPointShape (PointShape.SQUARE_FILLED_BIG);
////        connectionWidget.setRouter (RouterFactory.createOrthogonalSearchRouter (connectLayer));
////        connectionWidget.getActions ().addAction (ActionFactory.createAddRemoveControlPointAction ());
////        connectionWidget.getActions ().addAction (ActionFactory.createFreeMoveControlPointAction ());
////                connectionWidget.reroute ();
//        connectLayer.addChild(connectionWidget);
//
//        connectionWidget.setEndPointShape(PointShape.SQUARE_FILLED_BIG);
//        connectionWidget.getActions().addAction(createObjectHoverAction());
//        connectionWidget.getActions().addAction(createSelectAction());
//        connectionWidget.getActions().addAction(reconnectAction);
//        connectionWidget.getActions().addAction(new TransitionActions(getManager().getTransition(e),connectionWidget));
//        connectionWidget.getActions().addAction(ActionFactory.createPopupMenuAction(new TransitionMenuProvider(this,connectionWidget,e)));
//        return connectionWidget;
//    }
//
//    @Override
//    protected void attachEdgeSourceAnchor(String edge, String oldSourceNode, String sourceNode) {
//        LabeledConnectionWidget cw = ((LabeledConnectionWidget) findWidget(edge));
//        cw.setSourceAnchor(new OSlotAnchor((CapsuleViewUI) findWidget(sourceNode)));
//        cw.setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED);
//    }
//
//    @Override
//    protected void attachEdgeTargetAnchor(String edge, String oldTargetNode, String targetNode) {
//        ((LabeledConnectionWidget) findWidget(edge)).setTargetAnchor(new ISlotAnchor(((CapsuleViewUI) findWidget(targetNode)), this.currentSlotIndex));
//
//    }
//
//    @Override
//    public void setLayout() {
//        GraphLayout graphLayout = GraphLayoutFactory.createHierarchicalGraphLayout(this, true);
//        graphLayout.layoutGraph(this);
//        SceneLayout sceneGraphLayout = LayoutFactory.createSceneGraphLayout(this, graphLayout);
//        sceneGraphLayout.invokeLayout();
//    }
//
//    public void build(IMole mole) throws InternalProcessingError, UserBadDataError, Throwable {
//
//        setLayout();
//
////        mole.visit(new IVisitor<IGenericCapsule>() {
////
////            int i = 0;
////
////            @Override
////            public void action(IGenericCapsule visited) throws InternalProcessingError, UserBadDataError {
////                String nodeID, startNodeId;
////                Iterator<ISlot> itG = (Iterator<ISlot>) visited.intputSlots();
////                while (itG.hasNext()) {
////                    ISlot its = itG.next();
////                    nodeID = getConnectableName(visited);
////                    buildXXTasks(visited, nodeID);
////                    Iterator<IGenericTransition> itT = (Iterator<IGenericTransition>) its.transitions();
////                    while (itT.hasNext()) {
////                        IGenericTransition transition = itT.next();
////                        startNodeId = getConnectableName(transition.start());
////                        buildXXTasks(transition.start(), startNodeId);
////                        createEdge(startNodeId,
////                                nodeID);
////                    }
////                }
////
////            }
////        });
//        validate();
//    }
//
//    private String getConnectableName(IGenericCapsule tCapsule) {
//        String nodeID = "";
//        Option<IGenericTask> opt = tCapsule.task();
//        if (opt.isDefined()) {
//            nodeID = String.valueOf(opt.get().name());
//        }
//        return nodeID;
//    }
//
//    private void buildXXTasks(IGenericCapsule tCapsule,
//            String nodeID) throws InternalProcessingError, UserBadDataError {
//
//        if (tCapsule.task() != null) {
//            if (!getNodes().contains(nodeID)) {
//            }
//        } else {
//            if (!getNodes().contains(nodeID)) {
//             //   UIFactory.getInstance().createCapsule(this);
//            }
//        }
//    }
//
//    @Override
//    public void refresh() {
//        validate();
//        repaint();
//    }
//
//    private class MoleSceneConnectProvider implements ConnectProvider {
//
//        private String source = null;
//        private String target = null;
//
//        @Override
//        public boolean isSourceWidget(Widget sourceWidget) {
//            Object object = findObject(sourceWidget);
//            source = isNode(object) ? (String) object : null;
//
//            if (sourceWidget.getClass().equals(CapsuleViewUI.class)) {
//                return source != null;
//            }
//            return false;
//        }
//
//        @Override
//        public ConnectorState isTargetWidget(Widget sourceWidget, Widget targetWidget) {
//            Object object = findObject(targetWidget);
//            target = isNode(object) ? (String) object : null;
//            if (targetWidget.getClass().equals(ISlotWidget.class)) {
//                ISlotWidget iw = (ISlotWidget) targetWidget;
//                if (!iw.isStartingSlot()) {
//                    MoleScene.this.currentSlotIndex = iw.getIndex();
//                    return !source.equals(target) ? ConnectorState.ACCEPT : ConnectorState.REJECT_AND_STOP;
//                }
//            }
//            return object != null ? ConnectorState.REJECT_AND_STOP : ConnectorState.REJECT;
//        }
//
//        @Override
//        public boolean hasCustomTargetWidgetResolver(Scene scene) {
//            return false;
//        }
//
//        @Override
//        public Widget resolveTargetWidget(Scene scene, Point sceneLocation) {
//            return null;
//        }
//
//        @Override
//        public void createConnection(Widget sourceWidget, Widget targetWidget) {
//            manager.registerTransition(new TransitionUI((CapsuleViewUI) sourceWidget, (ISlotWidget) targetWidget));
//            MoleScene.this.createEdge(source, target);
//        }
//    }
//
//    private class MoleSceneReconnectProvider implements ReconnectProvider {
//
//        String edge;
//        String originalNode;
//        String replacementNode;
//
//        @Override
//        public void reconnectingStarted(ConnectionWidget connectionWidget, boolean reconnectingSource) {
//        }
//
//        @Override
//        public void reconnectingFinished(ConnectionWidget connectionWidget, boolean reconnectingSource) {
//        }
//
//        @Override
//        public boolean isSourceReconnectable(ConnectionWidget connectionWidget) {
//            Object object = findObject(connectionWidget);
//            edge = isEdge(object) ? (String) object : null;
//            originalNode = edge != null ? getEdgeSource(edge) : null;
//            return originalNode != null;
//        }
//
//        @Override
//        public boolean isTargetReconnectable(ConnectionWidget connectionWidget) {
//            Object object = findObject(connectionWidget);
//            edge = isEdge(object) ? (String) object : null;
//            originalNode = edge != null ? getEdgeTarget(edge) : null;
//            return originalNode != null;
//        }
//
//        @Override
//        public ConnectorState isReplacementWidget(ConnectionWidget connectionWidget, Widget replacementWidget, boolean reconnectingSource) {
//            Object object = findObject(replacementWidget);
//            replacementNode = isNode(object) ? (String) object : null;
//            if (replacementWidget.getClass().equals(OSlotWidget.class)) {
//                OSlotWidget ow = (OSlotWidget) replacementWidget;
//                return ConnectorState.ACCEPT;
//            } else if (replacementWidget.getClass().equals(ISlotWidget.class)) {
//                ISlotWidget iw = (ISlotWidget) replacementWidget;
//                MoleScene.this.currentSlotIndex = iw.getIndex();
//                return ConnectorState.ACCEPT;
//            }
//            return object != null ? ConnectorState.REJECT_AND_STOP : ConnectorState.REJECT;
//        }
//
//        @Override
//        public boolean hasCustomReplacementWidgetResolver(Scene scene) {
//            return false;
//        }
//
//        @Override
//        public Widget resolveReplacementWidget(Scene scene, Point sceneLocation) {
//            return null;
//        }
//
//        @Override
//        public void reconnect(ConnectionWidget connectionWidget, Widget replacementWidget, boolean reconnectingSource) {
//            TransitionUI t = getManager().getTransition(edge);
//            getManager().removeTransition(edge);
//            if (replacementWidget == null) {
//                removeEdge(edge);
//            } else if (reconnectingSource) {
//                setEdgeSource(edge, replacementNode);
//                getManager().registerTransition(edge,new TransitionUI(((OSlotWidget) replacementWidget).getCapsuleView(), t.getTarget()));
//            } else {
//                ISlotWidget targetView =  ((ISlotWidget) replacementWidget);
//                connectionWidget.setTargetAnchor(new ISlotAnchor(targetView.getCapsuleView(), MoleScene.this.currentSlotIndex));
//                setEdgeTarget(edge, replacementNode);
//
//                getManager().registerTransition(edge, new TransitionUI(t.getSource(), targetView));
//            }
//            MoleScene.this.repaint();
//        }
//    }
//
//    public Image getImageFromTransferable(Transferable transferable) {
//        Object o = null;
//        try {
//            o = transferable.getTransferData(DataFlavor.imageFlavor);
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        } catch (UnsupportedFlavorException ex) {
//            ex.printStackTrace();
//        }
//        return o instanceof Image ? (Image) o : ImageUtilities.loadImage("ressources/shape1.png");
//    }