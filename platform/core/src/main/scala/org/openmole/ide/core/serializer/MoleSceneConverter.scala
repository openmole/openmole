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

package org.openmole.ide.core.serializer

import com.thoughtworks.xstream.converters.Converter
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import scala.collection.JavaConversions._
import java.awt.Point
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.commons.IOType
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.implementation.TransitionUI
import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget
import org.openmole.ide.core.exception.MoleExceptionManagement
import org.openmole.ide.core.workflow.model.ICapsuleView
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import org.openide.util.Lookup

class MoleSceneConverter extends Converter{
  override def marshal(o: Object,writer: HierarchicalStreamWriter,mc: MarshallingContext) = {
    
    var firstSlotID = new HashMap[ICapsuleView, Int]
    var iSlotMapping = new HashMap[ISlotWidget, Int]
    var taskUIs = new HashSet[TaskUI]
    
    val molescene= o.asInstanceOf[MoleScene]
    var slotcount = 0
    
    writer.addAttribute("name", molescene.manager.name.get)
    molescene.manager.capsuleViews.values.foreach(view=> {
        writer.startNode("capsule")
        writer.addAttribute("start", view.capsuleModel.startingCapsule.toString)
        writer.addAttribute("x", String.valueOf(view.connectableWidget.convertLocalToScene(view.connectableWidget.getLocation).getX))
        writer.addAttribute("y", String.valueOf(view.connectableWidget.convertLocalToScene(view.connectableWidget.getLocation).getY))

        //Input slot
        slotcount+= 1
        firstSlotID.put(view, slotcount)
        view.connectableWidget.islots.foreach(is=>{ 
            iSlotMapping += is-> slotcount
            writer.startNode("islot")
            writer.addAttribute("id",slotcount.toString)
            writer.endNode
            slotcount+= 1
          })
        
        //Output slot
        writer.startNode("oslot")
        writer.addAttribute("id", slotcount.toString)
        writer.endNode
        
        //Task
        if (view.capsuleModel.containsTask) {
          taskUIs.add(view.capsuleModel.taskUI.get)
          writer.startNode("task");
          writer.addAttribute("name", view.capsuleModel.taskUI.get.panelUIData.name)
         // writer.addAttribute("type", view.capsuleModel.taskModel.get.getType.getName.toString)
         // writer.addAttribute("type", view.capsuleModel.taskUI.get.factory)
          view.capsuleModel.taskUI.get.prototypesIn.foreach(proto=> {
              writer.startNode("iprototype")
              writer.addAttribute("name", proto.panelUIData.name);
    //          writer.addAttribute("type", proto.factory.coreClass.getName)
              writer.endNode
            })
               
          view.capsuleModel.taskUI.get.prototypesOut.foreach(proto=> {
              writer.startNode("oprototype")
              writer.addAttribute("name", proto.panelUIData.name)
         //     writer.addAttribute("type", proto.factory.coreClass.getName)
              writer.endNode();
            })
          writer.endNode
        }
        writer.endNode
      })
    //Transitions
    molescene.manager.getTransitions.foreach(trans=> {
        writer.startNode("transition");
        writer.addAttribute("source",(firstSlotID(trans.source) + trans.source.capsuleModel.nbInputSlots).toString)
        writer.addAttribute("target", iSlotMapping(trans.target).toString)
        writer.addAttribute("condition", trans.condition.getOrElse(""))
        writer.endNode
      })
  }
  
  override def unmarshal(reader: HierarchicalStreamReader,uc: UnmarshallingContext) =  {
    var oslots = new HashMap[String, ICapsuleView]
    var islots = new HashMap[String, ISlotWidget]
    
    val scene = new MoleScene
    scene.manager.name = Some(reader.getAttribute("name"))
        
    //Capsules
    while (reader.hasMoreChildren) {
      reader.moveDown
      val n0 = reader.getNodeName
      n0 match {
        case "capsule"=> {
            val p= new Point
            p.setLocation(reader.getAttribute("x").toDouble, reader.getAttribute("y").toDouble)
            val caps = MoleScenesManager.createCapsule(scene, p)
            while (reader.hasMoreChildren) {
              reader.moveDown
              val n1= reader.getNodeName
              n1 match{
                case "islot"=> islots.put(reader.getAttribute("id"), caps.addInputSlot)
                case "oslot"=> oslots.put(reader.getAttribute("id"), caps)
                case "task"=> {
                    val n = reader.getAttribute("name")
                  //  val taskType = reader.getAttribute("type")
                    //caps.encapsule(new TaskUI(n, Class.forName(taskType)))
                    while (reader.hasMoreChildren) {
                      reader.moveDown
                      val n2 = reader.getNodeName 
                    // n2 match {
                     //   case "iprototype"=>  caps.capsuleModel.taskUI.get.addPrototype(new PrototypeUI(reader.getAttribute("name"),Class.forName(reader.getAttribute("type"))), IOType.INPUT)                 
                     //   case "oprototype"=>  caps.capsuleModel.taskUI.get.addPrototype(new PrototypeUI(reader.getAttribute("name"),Class.forName(reader.getAttribute("type"))), IOType.OUTPUT)
                     //   case _=> MoleExceptionManagement.showException("Unknown balise "+ n2)
                    //  }
                      reader.moveUp                  
                    }
                  }
                case _=> MoleExceptionManagement.showException("Unknown balise "+ n1)
              }
              reader.moveUp
            }     
          }
        case "transition"=> {
            val source = oslots(reader.getAttribute("source"))
            val target = islots(reader.getAttribute("target"))
            val condition = reader.getAttribute("condition")
            scene.manager.registerTransition(new TransitionUI(source, target, Some(condition)))
            scene.createEdge(scene.manager.capsuleViewID(source), scene.manager.capsuleViewID(target.capsuleView))           
          }
        case _=> MoleExceptionManagement.showException("Unknown balise "+ n0)        
      }
      reader.moveUp
    }
        
    MoleScenesManager.display(scene)
    scene
  }
  
  override def canConvert(t: Class[_]) = t.equals(classOf[MoleScene])
}
//package org.openmole.ide.core.serializer;
//
//import com.thoughtworks.xstream.converters.Converter;
//import com.thoughtworks.xstream.converters.MarshallingContext;
//import com.thoughtworks.xstream.converters.UnmarshallingContext;
//import com.thoughtworks.xstream.io.HierarchicalStreamReader;
//import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
//import java.awt.Point;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.Map;
//import org.openmole.commons.exception.UserBadDataError;
//import org.openmole.ide.core.commons.IOType;
//import org.openmole.ide.core.control.MoleScenesManager;
//import org.openmole.ide.core.exception.MoleExceptionManagement;
//import org.openmole.ide.core.implementation.UIFactory;
//import org.openmole.ide.core.workflow.implementation.MoleScene;
//import org.openmole.ide.core.workflow.implementation.PrototypeUI;
//import org.openmole.ide.core.workflow.implementation.TaskUI;
//import org.openmole.ide.core.workflow.implementation.TransitionUI;
//import org.openmole.ide.core.workflow.implementation.paint.ISlotWidget;
//import org.openmole.ide.core.workflow.model.ICapsuleView;
//import org.openmole.ide.core.workflow.model.IGenericTaskModelUI;
//public class MoleSceneConverter implements Converter {
//
//    Collection<IGenericTaskModelUI> taskModels = new HashSet<IGenericTaskModelUI>();
//    private Map<ICapsuleView, Integer> firstSlotID = new HashMap<ICapsuleView, Integer>();
//    private Map<ISlotWidget, Integer> iSlotMapping = new HashMap<ISlotWidget, Integer>();
//
//    @Override
//    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext mc) {
//
//        MoleScene molescene = (MoleScene) o;
//        int slotcount = 0;
//
//        writer.addAttribute("name", molescene.getManager().getName());
//
//        for (Iterator<ICapsuleView> itV = molescene.getManager().getCapsuleViews().iterator(); itV.hasNext();) {
//            ICapsuleView view = itV.next();
//
//            writer.startNode("capsule");
//            writer.addAttribute("start", view.getCapsuleModel().isStartingCapsule() ? "true" : "false");
//            writer.addAttribute("x", String.valueOf(view.getConnectableWidget().convertLocalToScene(view.getConnectableWidget().getLocation()).getX()));
//            writer.addAttribute("y", String.valueOf(view.getConnectableWidget().convertLocalToScene(view.getConnectableWidget().getLocation()).getY()));
//
//            //Input slot
//            slotcount++;
//            firstSlotID.put(view, slotcount);
//            for (ISlotWidget iw : view.getConnectableWidget().getIslots()) {
//                iSlotMapping.put(iw, slotcount);
//                writer.startNode("islot");
//                writer.addAttribute("id", String.valueOf(slotcount));
//                writer.endNode();
//                slotcount++;
//            }
//
//            //Output slot
//            writer.startNode("oslot");
//            writer.addAttribute("id", String.valueOf(slotcount));
//            writer.endNode();
//
//            //Task
//            if (view.getCapsuleModel().containsTask()) {
//                taskModels.add(view.getCapsuleModel().getTaskModel());
//                writer.startNode("task");
//                writer.addAttribute("name", view.getCapsuleModel().getTaskModel().getName());
//                writer.addAttribute("type", view.getCapsuleModel().getTaskModel().getType().getName().toString());
//                for (Iterator<PrototypeUI> ipit = view.getCapsuleModel().getTaskModel().getPrototypesIn().iterator(); ipit.hasNext();) {
//                    PrototypeUI proto = ipit.next();
//                    writer.startNode("iprototype");
//                    writer.addAttribute("name", proto.getName());
//                    writer.addAttribute("type", proto.getType().getName().toString());
//                    writer.endNode();
//                }
//                for (Iterator<PrototypeUI> opit = view.getCapsuleModel().getTaskModel().getPrototypesOut().iterator(); opit.hasNext();) {
//                    PrototypeUI proto = opit.next();
//                    writer.startNode("oprototype");
//                    writer.addAttribute("name", proto.getName());
//                    writer.addAttribute("type", proto.getType().getName().toString());
//                    writer.endNode();
//                }
//                writer.endNode();
//            }
//            writer.endNode();
//        }
//
//        //Transitions
//        for (TransitionUI trans : molescene.getManager().getTransitions()) {
//            writer.startNode("transition");
//            writer.addAttribute("source", String.valueOf(firstSlotID.get(trans.getSource()) + trans.getSource().getCapsuleModel().getNbInputslots()));
//            writer.addAttribute("target", String.valueOf(iSlotMapping.get(trans.getTarget())));
//            writer.addAttribute("condition", trans.getCondition());
//            writer.endNode();
//        }
//
//    }
//
//    @Override
//    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext uc) {
//
//        Map<String, ICapsuleView> oslots = new HashMap<String, ICapsuleView>();
//        Map<String, ISlotWidget> islots = new HashMap<String, ISlotWidget>();
//
//        MoleScene scene = new MoleScene();
//        scene.getManager().setName(reader.getAttribute("name"));
//        //Capsules
//        while (reader.hasMoreChildren()) {
//            reader.moveDown();
//            if ("capsule".equals(reader.getNodeName())) {
//                Point p = new Point();
//                p.setLocation(Double.parseDouble(reader.getAttribute("x")), Double.parseDouble(reader.getAttribute("y")));
//                ICapsuleView caps = UIFactory.createCapsule(scene, p);
//                // Slots
//                while (reader.hasMoreChildren()) {
//                    reader.moveDown();
//                    if ("islot".equals(reader.getNodeName())) {
//                        islots.put(reader.getAttribute("id"), caps.addInputSlot());
//                    } else if ("oslot".equals(reader.getNodeName())) {
//                        oslots.put(reader.getAttribute("id"), caps);
//                    } else if ("task".equals(reader.getNodeName())) {
//                        String n = reader.getAttribute("name");
//                        String taskType = reader.getAttribute("type");
//                        try {
//                            caps.encapsule(new TaskUI(n, Class.forName(taskType)));
//                        } catch (UserBadDataError ex) {
//                            MoleExceptionManagement.showException("No graphical implementation for the task class " + taskType + ". The encapsulation is net possible");
//                        } catch (ClassNotFoundException ex) {
//                            MoleExceptionManagement.showException("Unknown task class " + taskType);
//                        }
//                        while (reader.hasMoreChildren()) {
//                            reader.moveDown();
//                            if ("iprototype".equals(reader.getNodeName()) || "oprototype".equals(reader.getNodeName())) {
//                                try {
//                                    caps.getCapsuleModel().getTaskModel().addPrototype(new PrototypeUI(reader.getAttribute("name"),
//                                            Class.forName(reader.getAttribute("type"))), "iprototype".equals(reader.getNodeName()) ? IOType.INPUT : IOType.OUTPUT);
//                                } catch (ClassNotFoundException ex) {
//                                    MoleExceptionManagement.showException("The prototype class " + reader.getAttribute("type") + "does not exist.");
//                                }
//                            }
//                            reader.moveUp();
//                        }
//                    }
//                    reader.moveUp();
//                }
//            } else if ("transition".equals(reader.getNodeName())) {
//                ICapsuleView source = oslots.get(reader.getAttribute("source"));
//                ISlotWidget target = islots.get(reader.getAttribute("target"));
//                String condition = reader.getAttribute("condition");
//
//                scene.getManager().registerTransition(new TransitionUI(source, target, condition));
//
//                scene.createEdge(scene.getManager().getCapsuleViewID(source), scene.getManager().getCapsuleViewID(target.getCapsuleView()));
//            }
//            reader.moveUp();
//        }
//        MoleScenesManager.getInstance().display(scene);
//        return scene;
//    }
//
//    @Override
//    public boolean canConvert(Class type) {
//        return type.equals(MoleScene.class);
//    }
//}
