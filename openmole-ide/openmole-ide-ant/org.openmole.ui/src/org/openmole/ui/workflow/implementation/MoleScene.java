/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.workflow.implementation;

import org.openmole.ui.workflow.provider.MoleSceneMenuProvider;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Iterator;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectProvider;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.ReconnectProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.anchor.AnchorShape;
import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.graph.layout.GraphLayout;
import org.netbeans.api.visual.graph.layout.GraphLayoutFactory;
import org.netbeans.api.visual.layout.LayoutFactory;
import org.netbeans.api.visual.layout.SceneLayout;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.ImageUtilities;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.model.ITaskCapsuleView;
import org.openmole.ui.workflow.model.IMoleScene;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.model.capsule.IGenericTaskCapsule;
import org.openmole.core.model.mole.IMole;
import org.openmole.core.model.transition.ITransition;
import org.openmole.core.model.transition.ITransitionSlot;
import org.openmole.ui.control.MoleScenesManager;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.provider.DnDNewTaskCapsuleProvider;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class MoleScene extends GraphScene.StringGraph implements IMoleScene {

    public static final String MOVE = "move";
    public static final String CONNECT = "connect";
    public static final String RECONNECT = "connect";
    private MoleSceneManager manager = new MoleSceneManager();
    private LayerWidget taskLayer = new LayerWidget(this);
    private LayerWidget taskCapsuleLayer = new LayerWidget(this);
    private LayerWidget taskCompositeLayer = new LayerWidget(this);
    private LayerWidget connectLayer = new LayerWidget(this);
    private LayerWidget currentLayer = null;
    private Widget obUI = null;
    private int nbEdges = 0;
    private WidgetAction connectAction = ActionFactory.createConnectAction(connectLayer, new MoleSceneConnectProvider());
    private WidgetAction reconnectAction = ActionFactory.createReconnectAction(new MoleSceneReconnectProvider());
    private WidgetAction moveAction = ActionFactory.createMoveAction();
    private boolean detailedView = false;

    public MoleScene() {
        super();
        addChild(taskCapsuleLayer);
        addChild(taskLayer);
        addChild(taskCompositeLayer);
        addChild(connectLayer);

        setPreferredSize(new Dimension((int) (ApplicationCustomize.SCREEN_WIDTH * 0.8), (int) (ApplicationCustomize.SCREEN_HEIGHT * 0.8)));
        // view = createView();
        getActions().addAction(ActionFactory.createPopupMenuAction(new MoleSceneMenuProvider(this)));
        //getActions().addAction(ActionFactory.createRectangularSelectAction(this, taskLayer));
        //getActions().addAction(ActionFactory.createRectangularSelectAction(this, taskCapsuleLayer));
        // getActions().addAction(ActionFactory.createZoomAction());
        //  getActions().addAction(ActionFactory.createPanAction());

        getActions().addAction(ActionFactory.createAcceptAction(new DnDNewTaskCapsuleProvider(this)));

        //getActions().addAction(ActionFactory.createAcceptAction(new DnDProvider(this)));

        setMovable(true);
        MoleScenesManager.getInstance().addMoleScene(this);
    }

    public void setDetailedView(boolean detailedView) {
        this.detailedView = detailedView;
    }

    public boolean isDetailedView() {
        return detailedView;
    }

    @Override
    public MoleSceneManager getManager() {
        return manager;
    }

    public void createEdge(String sourceNodeID, String targetNodeID) {
        String ed = "edge" + String.valueOf(nbEdges++);
        addEdge(ed);
        setEdgeSource(ed, sourceNodeID);
        setEdgeTarget(ed, targetNodeID);
    }

    public void removeElement(ICapsuleModelUI cm) {
        //    removeNode(getManager().getTaskViewID(cm));
        //    getManager().removeTaskView(cm);
    }

    @Override
    protected Widget attachNodeWidget(String n) {

        currentLayer.addChild(obUI);
        obUI.createActions(CONNECT).addAction(connectAction);
        obUI.getActions().addAction(createObjectHoverAction());
        //obUI.getActions ().addAction (createSelectAction());
        return obUI;
    }

    @Override
    protected Widget attachEdgeWidget(String e) {
        ConnectionWidget connectionWidget = new ConnectionWidget(this);
        connectLayer.addChild(connectionWidget);
        connectionWidget.createActions(RECONNECT).addAction(reconnectAction);

        // connectionWidget.setRouter (RouterFactory.createOrthogonalSearchRouter(connectLayer));

        //   connectionWidget.setRoutingPolicy(ConnectionWidget.RoutingPolicy.ALWAYS_ROUTE);
        return connectionWidget;
    }

    @Override
    protected void attachEdgeSourceAnchor(String edge, String oldSourceNode, String sourceNode) {
        ConnectionWidget cw = ((ConnectionWidget) findWidget(edge));
        cw.setSourceAnchor(((ITaskCapsuleView) (findWidget(sourceNode))).getConnectableWidget().getOutputSlotAnchor(0));
        cw.setTargetAnchorShape(AnchorShape.TRIANGLE_FILLED);
    }

    @Override
    protected void attachEdgeTargetAnchor(String edge, String oldTargetNode, String targetNode) {
        ConnectionWidget cw = ((ConnectionWidget) findWidget(edge));
        cw.setTargetAnchor(((ITaskCapsuleView) (findWidget(targetNode))).getConnectableWidget().getInputSlotAnchor(0));
    }

    @Override
    public void setLayout() {
        GraphLayout graphLayout = GraphLayoutFactory.createHierarchicalGraphLayout(this, true);
        graphLayout.layoutGraph(this);
        SceneLayout sceneGraphLayout = LayoutFactory.createSceneGraphLayout(this, graphLayout);
        sceneGraphLayout.invokeLayout();
    }

    public void build(IMole mole) throws InternalProcessingError, UserBadDataError {

        setLayout();

        mole.visit(new IVisitor<IGenericTaskCapsule>() {

            int i = 0;

            @Override
            public void action(IGenericTaskCapsule visited) throws InternalProcessingError, UserBadDataError {
                String nodeID, startNodeId;
                Iterator<ITransitionSlot> itG = visited.getIntputTransitionsSlots().iterator();
                while (itG.hasNext()) {
                    ITransitionSlot its = itG.next();
                    nodeID = getConnectableName(visited);
                    buildXXTasks(visited, nodeID);
                    Iterator<ITransition> itT = its.getTransitions().iterator();
                    while (itT.hasNext()) {
                        ITransition transition = itT.next();
                        startNodeId = getConnectableName(transition.getStart());
                        buildXXTasks(transition.getStart(), startNodeId);
                        createEdge(startNodeId,
                                nodeID);
                    }
                }

            }
        });
        validate();
    }

    private String getConnectableName(IGenericTaskCapsule tCapsule) {
        String nodeID = "";
        if (tCapsule.getTask() != null) {
            nodeID = String.valueOf(tCapsule.getTask().getName());
        }
        return nodeID;
    }

    private void buildXXTasks(IGenericTaskCapsule tCapsule,
            String nodeID) throws InternalProcessingError, UserBadDataError {
        ITaskCapsuleView connectable = null;

        if (tCapsule.getTask() != null) {
            if (!getNodes().contains(nodeID)) {
                //  connectable = createTaskComposite(tCapsule, nodeID);
                //  connectable = UIFactory.getInstance().createTaskComposite(this, tCapsule);
                //  initConnectable(tCapsule, connectable);
            }
        } else {
            if (!getNodes().contains(nodeID)) {
                /* if (nodeID.isEmpty()) {
                connectable = createTaskCapsule(nodeID);
                } else {
                connectable = createTaskCapsule();
                }*/
                UIFactory.getInstance().createTaskCapsule(this);
                //   initConnectable(tCapsule, connectable);
            }
        }
    }

    /*  private void initConnectable(IGenericTaskCapsule tCapsule,
    IConnectable connectable) {
    for (int i = 0; i < tCapsule.getIntputTransitionsSlots().size(); ++i) {
    connectable.addInputSlot();
    connectable.addOutputSlot();
    }
    connectable.setTaskCapsule(tCapsule);
    }*/
    @Override
    public void refresh() {
        validate();
        repaint();
    }

    @Override
    public void setMovable(boolean b) {
        setActiveTool(null);
        if (b) {
            setActiveTool(MOVE);
        } else {
            setActiveTool(CONNECT);
            setActiveTool(RECONNECT);
        }
    }

    @Override
    public void initCompositeAdd(Widget w) {
        currentLayer = taskCompositeLayer;
        obUI = w;
    }

    @Override
    public void initCapsuleAdd(Widget w) {
        currentLayer = taskCapsuleLayer;
        obUI = w;
    }

    private class MoleSceneConnectProvider implements ConnectProvider {

        private String source = null;
        private String target = null;

        @Override
        public boolean isSourceWidget(Widget sourceWidget) {
            Object object = findObject(sourceWidget);
            source = isNode(object) ? (String) object : null;
            return source != null;
        }

        @Override
        public ConnectorState isTargetWidget(Widget sourceWidget, Widget targetWidget) {
            Object object = findObject(targetWidget);
            target = isNode(object) ? (String) object : null;
            if (target != null) {
                return !source.equals(target) ? ConnectorState.ACCEPT : ConnectorState.REJECT_AND_STOP;
            }
            return object != null ? ConnectorState.REJECT_AND_STOP : ConnectorState.REJECT;
        }

        @Override
        public boolean hasCustomTargetWidgetResolver(Scene scene) {
            return false;
        }

        @Override
        public Widget resolveTargetWidget(Scene scene, Point sceneLocation) {
            return null;
        }

        @Override
        public void createConnection(Widget sourceWidget, Widget targetWidget) {
            MoleScene.this.createEdge(source, target);
        }
    }

    private class MoleSceneReconnectProvider implements ReconnectProvider {

        String edge;
        String originalNode;
        String replacementNode;

        @Override
        public void reconnectingStarted(ConnectionWidget connectionWidget, boolean reconnectingSource) {
        }

        @Override
        public void reconnectingFinished(ConnectionWidget connectionWidget, boolean reconnectingSource) {
        }

        @Override
        public boolean isSourceReconnectable(ConnectionWidget connectionWidget) {
            Object object = findObject(connectionWidget);
            edge = isEdge(object) ? (String) object : null;
            originalNode = edge != null ? getEdgeSource(edge) : null;
            return originalNode != null;
        }

        @Override
        public boolean isTargetReconnectable(ConnectionWidget connectionWidget) {
            Object object = findObject(connectionWidget);
            edge = isEdge(object) ? (String) object : null;
            originalNode = edge != null ? getEdgeTarget(edge) : null;
            return originalNode != null;
        }

        @Override
        public ConnectorState isReplacementWidget(ConnectionWidget connectionWidget, Widget replacementWidget, boolean reconnectingSource) {
            Object object = findObject(replacementWidget);
            replacementNode = isNode(object) ? (String) object : null;
            if (replacementNode != null) {
                return ConnectorState.ACCEPT;
            }
            return object != null ? ConnectorState.REJECT_AND_STOP : ConnectorState.REJECT;
        }

        @Override
        public boolean hasCustomReplacementWidgetResolver(Scene scene) {
            return false;
        }

        @Override
        public Widget resolveReplacementWidget(Scene scene, Point sceneLocation) {
            return null;
        }

        @Override
        public void reconnect(ConnectionWidget connectionWidget, Widget replacementWidget, boolean reconnectingSource) {
            if (replacementWidget == null) {
                removeEdge(edge);
            } else if (reconnectingSource) {
                setEdgeSource(edge, replacementNode);
            } else {
                setEdgeTarget(edge, replacementNode);
            }
            MoleScene.this.repaint();
        }
    }

    public Image getImageFromTransferable(Transferable transferable) {
        Object o = null;
        try {
            o = transferable.getTransferData(DataFlavor.imageFlavor);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (UnsupportedFlavorException ex) {
            ex.printStackTrace();
        }
        return o instanceof Image ? (Image) o : ImageUtilities.loadImage("ressources/shape1.png");
    }
}
