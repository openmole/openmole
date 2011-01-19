/*
 *  Copyright (C) 2011 Mathieu Leclaire <mathieu.leclaire@openmole.org>
 * 
 *  This program is free software: you can redistribute itV and/or modify
 *  itV under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that itV will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ui.ide.serializer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.awt.Point;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.commons.IOType;
import org.openmole.ui.ide.control.MoleScenesManager;
import org.openmole.ui.ide.exception.MoleExceptionManagement;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.TaskUI;
import org.openmole.ui.ide.workflow.implementation.TransitionUI;
import org.openmole.ui.ide.workflow.implementation.UIFactory;
import org.openmole.ui.ide.workflow.implementation.paint.ISlotWidget;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class MoleSceneConverter implements Converter {

    Collection<IGenericTaskModelUI> taskModels = new HashSet<IGenericTaskModelUI>();
    private Map<ICapsuleView, Integer> firstSlotID = new HashMap<ICapsuleView, Integer>();
    private Map<ISlotWidget, Integer> iSlotMapping = new HashMap<ISlotWidget, Integer>();

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {

        MoleScene molescene = (MoleScene) o;
        int slotcount = 0;

        writer.addAttribute("name", molescene.getManager().getName());

        for (Iterator<ICapsuleView> itV = molescene.getManager().getCapsuleViews().iterator(); itV.hasNext();) {
            ICapsuleView view = itV.next();

            writer.startNode("capsule");
            writer.addAttribute("start", view.getCapsuleModel().isStartingCapsule() ? "true" : "false");
            writer.addAttribute("x", String.valueOf(view.getConnectableWidget().convertLocalToScene(view.getConnectableWidget().getLocation()).getX()));
            writer.addAttribute("y", String.valueOf(view.getConnectableWidget().convertLocalToScene(view.getConnectableWidget().getLocation()).getY()));

            //Input slot
            slotcount++;
            firstSlotID.put(view, slotcount);
            for (ISlotWidget iw : view.getConnectableWidget().getIslots()) {
                iSlotMapping.put(iw, slotcount);
                writer.startNode("islot");
                writer.addAttribute("id", String.valueOf(slotcount));
                writer.endNode();
                slotcount++;
            }

            //Output slot
            writer.startNode("oslot");
            writer.addAttribute("id", String.valueOf(slotcount));
            writer.endNode();

            //Task
            if (view.getCapsuleModel().containsTask()) {
                taskModels.add(view.getCapsuleModel().getTaskModel());
                writer.startNode("task");
                writer.addAttribute("name", view.getCapsuleModel().getTaskModel().getName());
                writer.addAttribute("type", view.getCapsuleModel().getTaskModel().getType().getName().toString());
                for (Iterator<PrototypeUI> ipit = view.getCapsuleModel().getTaskModel().getPrototypesIn().iterator(); ipit.hasNext();) {
                    PrototypeUI proto = ipit.next();
                    writer.startNode("iprototype");
                    writer.addAttribute("name", proto.getName());
                    writer.addAttribute("type", proto.getType().getName().toString());
                    writer.endNode();
                }
                for (Iterator<PrototypeUI> opit = view.getCapsuleModel().getTaskModel().getPrototypesOut().iterator(); opit.hasNext();) {
                    PrototypeUI proto = opit.next();
                    writer.startNode("oprototype");
                    writer.addAttribute("name", proto.getName());
                    writer.addAttribute("type", proto.getType().getName().toString());
                    writer.endNode();
                }
                writer.endNode();
            }
            writer.endNode();
        }

        //Transitions
        for (TransitionUI trans : molescene.getManager().getTransitions()) {
            writer.startNode("transition");
            writer.addAttribute("source", String.valueOf(firstSlotID.get(trans.getSource()) + trans.getSource().getCapsuleModel().getNbInputslots()));
            writer.addAttribute("target", String.valueOf(iSlotMapping.get(trans.getTarget())));
            writer.endNode();
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {

        Map<String, ICapsuleView> oslots = new HashMap<String, ICapsuleView>();
        Map<String, ISlotWidget> islots = new HashMap<String, ISlotWidget>();

        MoleScene scene = new MoleScene();
        scene.getManager().setName(reader.getAttribute("name"));
        //Capsules
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if ("capsule".equals(reader.getNodeName())) {
                Point p = new Point();
                p.setLocation(Double.parseDouble(reader.getAttribute("x")), Double.parseDouble(reader.getAttribute("y")));
                ICapsuleView caps = UIFactory.getInstance().createCapsule(scene, p);
                // Slots
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    if ("islot".equals(reader.getNodeName())) {
                        islots.put(reader.getAttribute("id"), caps.addInputSlot());
                    } else if ("oslot".equals(reader.getNodeName())) {
                        oslots.put(reader.getAttribute("id"), caps);
                    } else if ("task".equals(reader.getNodeName())) {
                        String n = reader.getAttribute("name");
                        String taskType = reader.getAttribute("type");
                        try {
                            caps.encapsule(new TaskUI(n, Class.forName(taskType)));
                        } catch (UserBadDataError ex) {
                            MoleExceptionManagement.showException("No graphical implementation for the task class " + taskType + ". The encapsulation is net possible");
                        } catch (ClassNotFoundException ex) {
                            MoleExceptionManagement.showException("Unknown task class " + taskType);
                        }
                        while (reader.hasMoreChildren()) {
                            reader.moveDown();
                            if ("iprototype".equals(reader.getNodeName()) || "oprototype".equals(reader.getNodeName())) {
                                try {
                                    caps.getCapsuleModel().getTaskModel().addPrototype(new PrototypeUI(reader.getAttribute("name"),
                                            Class.forName(reader.getAttribute("type"))), "iprototype".equals(reader.getNodeName()) ? IOType.INPUT : IOType.OUTPUT);
                                } catch (ClassNotFoundException ex) {
                                    MoleExceptionManagement.showException("The prototype class " + reader.getAttribute("type") + "does not exist.");
                                }
                            }
                            reader.moveUp();
                        }
                    }
                    reader.moveUp();
                }
            } else if ("transition".equals(reader.getNodeName())) {
                ICapsuleView source = oslots.get(reader.getAttribute("source"));
                ISlotWidget target = islots.get(reader.getAttribute("target"));

                scene.getManager().addTransition(source, target);
                scene.createEdge(scene.getManager().getCapsuleViewID(source), scene.getManager().getCapsuleViewID(target.getCapsuleView()));
            }
            reader.moveUp();
        }
        MoleScenesManager.getInstance().display(scene);
        return scene;
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(MoleScene.class);
    }
}
