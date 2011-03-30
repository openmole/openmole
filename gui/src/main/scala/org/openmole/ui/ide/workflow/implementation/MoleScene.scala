/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import java.awt.Dimension
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.graph.GraphScene
import org.openmole.ui.ide.commons.ApplicationCustomize
import org.openmole.ui.ide.provider.MoleSceneMenuProvider
import org.openmole.ui.ide.workflow.model.IMoleScene
import org.netbeans.api.visual.widget.LayerWidget
import org.openide.util.ImageUtilities

class MoleScene extends GraphScene.StringGraph with IMoleScene{
  
  val manager = new MoleSceneManager
  val MOVE= "move"
  val capsuleLayer= new LayerWidget(this)
  val connectLayer = new LayerWidget(this)
  
  
  addChild(capsuleLayer)
  addChild(connectLayer)
  
  setPreferredSize(new Dimension((ApplicationCustomize.SCREEN_WIDTH * 0.8).toInt, (ApplicationCustomize.SCREEN_HEIGHT * 0.8).toInt));
  getActions.addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)))
  
  
  def getImageFromTransferable(transferable: Transferable): Image= {
    try{
      transferable.getTransferData(DataFlavor.imageFlavor) match {
        case o: Image  => o
      }
    } catch {case _ => ImageUtilities.loadImage("ressources/shape1.png")}
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
//        setPreferredSize(new Dimension((int) (ApplicationCustomize.SCREEN_WIDTH * 0.8), (int) (ApplicationCustomize.SCREEN_HEIGHT * 0.8)));
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
//}
