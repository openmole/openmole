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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.netbeans.api.visual.widget.Scene;
import org.openmole.ui.ide.control.MoleScenesManager;
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
import org.openmole.ui.ide.workflow.implementation.MoleScene;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.PrototypesUI;
import org.openmole.ui.ide.workflow.implementation.TransitionUI;
import org.openmole.ui.ide.workflow.implementation.UIFactory;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;
import org.openmole.ui.ide.workflow.model.ICapsuleView;
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI;
import org.openmole.ui.ide.workflow.model.IMoleScene;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
 */
public class MoleSceneConverter implements Converter {

    Collection<IGenericTaskModelUI> taskModels = new ArrayList<IGenericTaskModelUI>();
    private Map<ICapsuleModelUI, Integer> firstSlotIDMapping = new HashMap<ICapsuleModelUI, Integer>();

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {

        for (Iterator<IMoleScene> itms = MoleScenesManager.getInstance().getMoleScenes().iterator(); itms.hasNext();) {
            int slotcount = 0;

            IMoleScene molescene = itms.next();

            //Mole
            writer.startNode("molescene");
            writer.addAttribute("name", molescene.getManager().getName());
            for (Iterator<ICapsuleView> itV = molescene.getManager().getCapsuleViews().iterator(); itV.hasNext();) {
                ICapsuleView view = itV.next();

                writer.startNode("capsule");

                writer.addAttribute("start", view.getCapsuleModel().isStartingCapsule() ? "true" : "false");

                writer.addAttribute("x", String.valueOf(view.getConnectableWidget().convertLocalToScene(view.getConnectableWidget().getLocation()).getX()));
                writer.addAttribute("y", String.valueOf(view.getConnectableWidget().convertLocalToScene(view.getConnectableWidget().getLocation()).getY()));

                //Input slot
                slotcount++;
                firstSlotIDMapping.put(view.getCapsuleModel(), slotcount);
                for (int is = 0; is < view.getCapsuleModel().getNbInputslots(); is++) {

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
                    writer.endNode();
                }
                writer.endNode();
            }

            //Transitions
            writer.startNode("transitions");
            for (Iterator<TransitionUI> itT = molescene.getManager().getTransitions().iterator(); itT.hasNext();) {
                TransitionUI trans = itT.next();
                writer.startNode("transition");
                writer.addAttribute("source", String.valueOf(firstSlotIDMapping.get(trans.getSource()) + trans.getSource().getNbInputslots()));
                writer.addAttribute("target", String.valueOf(firstSlotIDMapping.get(trans.getTarget()) + trans.getTargetSlotNumber()));
                writer.endNode();
            }
            writer.endNode();
            writer.endNode();
        }
        //Tasks
        writer.startNode("tasks");
        for (Iterator<IGenericTaskModelUI> itTM = taskModels.iterator(); itTM.hasNext();) {
            IGenericTaskModelUI t = itTM.next();
            writer.startNode("task");
            writer.addAttribute("name", t.getName());
            writer.addAttribute("impl", t.getClass().toString());
            for (Iterator<PrototypeUI> ipit = t.getPrototypesIn().iterator(); ipit.hasNext();) {
                writer.startNode("iprototype");
                writer.addAttribute("name", ipit.next().getName());
                writer.endNode();
            }
            for (Iterator<PrototypeUI> opit = t.getPrototypesOut().iterator(); opit.hasNext();) {
                writer.startNode("oprototype");
                writer.addAttribute("name", opit.next().getName());
                writer.endNode();
            }
            writer.endNode();
        }
        writer.endNode();

        //Prototypes
        writer.startNode("prototypes");
        for (Iterator<PrototypeUI> proto = PrototypesUI.getInstance().getPrototypes().iterator(); proto.hasNext();) {
            PrototypeUI p = proto.next();
            writer.startNode("prototype");
            writer.addAttribute("name", p.getName());
            writer.addAttribute("type", p.getType().getName().toString());
            writer.endNode();
        }
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {

        System.out.println("+ " + reader.getNodeName());
        //Molescenes
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            System.out.println("++ " + reader.getNodeName());
            if ("molescene".equals(reader.getNodeName())) {
                MoleScene scene = new MoleScene();
                scene.getManager().setName(reader.getAttribute("name"));
                //Capsules
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    System.out.println("+++ " + reader.getNodeName());
                    if ("capsule".equals(reader.getNodeName())) {
                        System.out.println("CAPSULE");
                        Point p = new Point();
                        p.setLocation(Double.parseDouble(reader.getAttribute("x")), Double.parseDouble(reader.getAttribute("y")));
                        ICapsuleView caps = UIFactory.getInstance().createCapsule(scene, p);
                        // Slots
                        int nbslots = 0;
                        while (reader.hasMoreChildren()) {
                            reader.moveDown();
                            if ("islot".equals(reader.getNodeName())) {
                                System.out.println("++++ " + reader.getNodeName());
                                nbslots++;
                            }
                            reader.moveUp();
                        }
                        for(int i=1;i<nbslots;++i)
                                caps.addInputSlot();
                        reader.moveUp();
                    }
                    MoleScenesManager.getInstance().display(scene);
                }
                reader.moveUp();
            }
        }
        return new Object();
    }

    @Override
    public boolean canConvert(Class type) {
        return type.equals(GUISerializer.OpenMole.class);
    }
}
