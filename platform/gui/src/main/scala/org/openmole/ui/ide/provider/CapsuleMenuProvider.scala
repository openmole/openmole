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

package org.openmole.ui.ide.provider

import java.awt.Point
import javax.swing.JMenu
import javax.swing.JMenuItem
import org.netbeans.api.visual.widget.Widget
import scala.collection.mutable.HashSet
import org.openmole.ui.ide.commons.IOType
import org.openmole.ui.ide.workflow.action.AddExistingPrototypeAction
import org.openmole.ui.ide.workflow.action.AddExistingPrototypeAction
import org.openmole.ui.ide.workflow.implementation.MoleScene
import org.openmole.ui.ide.workflow.action.AddInputSlotAction
import org.openmole.ui.ide.workflow.action.AddTaskAction
import org.openmole.ui.ide.workflow.action.DefineMoleStartAction
import org.openmole.ui.ide.workflow.action.RemoveCapsuleAction
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI
import org.openmole.ui.ide.workflow.implementation.PrototypesUI
import org.openmole.ui.ide.workflow.implementation.TaskUI
import org.openmole.ui.ide.workflow.implementation.TasksUI
import scala.collection.mutable.ListBuffer

class CapsuleMenuProvider(scene: MoleScene, capsuleView: CapsuleViewUI) extends GenericMenuProvider {
  var encapsulated= false
  var inPrototypeMenu= new JMenu
  var outPrototypeMenu= new JMenu
  var taskMenu= new JMenu
  
  val itIS= new JMenuItem("Add an input slot")
  val itR = new JMenuItem("Remove capsule")
  val itStart = new JMenuItem("Define as starting capsule")
  itIS.addActionListener(new AddInputSlotAction(capsuleView))
  itR.addActionListener(new RemoveCapsuleAction(scene,capsuleView))
  itStart.addActionListener(new DefineMoleStartAction(scene, capsuleView))
  
  items+= (itIS,itR,itStart)
  
  def addTaskMenus= encapsulated= true
  
  override def getPopupMenu(widget: Widget, point: Point)= {
    if (encapsulated) {
      val colI= fillPrototypeMenu(IOType.INPUT)
      val colO= fillPrototypeMenu(IOType.OUTPUT)
      if (! colI.isEmpty){
        menus.remove(inPrototypeMenu)  
        menus.remove(outPrototypeMenu)    
        inPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an input prototype ", colI)
        outPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an output prototype ", colO)
        menus.add(inPrototypeMenu);
        menus.add(outPrototypeMenu)
      }
      if (! TasksUI.getAll.isEmpty){
         menus.remove(taskMenu)
         var colTask = new ListBuffer[JMenuItem]
         TasksUI.getAll.foreach(t=> {
           val it= new JMenuItem(t.name + " :: " + t.entityType.getSimpleName)
           it.addActionListener(new AddTaskAction(scene,capsuleView, t.asInstanceOf[TaskUI]));
           colTask+= it
          })
      }
    }
    
    super.getPopupMenu(widget, point)
  }
  
  def fillPrototypeMenu(t: IOType.Value)= {
    val prototypeCol = HashSet.empty[JMenuItem]
    PrototypesUI.getAll.foreach(p=> {
        val it= new JMenuItem(p.name + " :: " + p.entityType.getSimpleName);
        it.addActionListener(new AddExistingPrototypeAction(p, capsuleView, t));
        prototypeCol.add(it)})
    prototypeCol.toSet
  }
}

//public JPopupMenu getPopupMenu(Widget widget, Point point) {
//        //Update prototypes
//        if (encapsulated) {
//            Collection<JMenuItem> colI = fillPrototypeMenu(IOType.INPUT);
//            Collection<JMenuItem> colO = fillPrototypeMenu(IOType.OUTPUT);
//            if (!colI.isEmpty()) {
//                menus.remove(inPrototypeMenu);
//                menus.remove(outPrototypeMenu);
//                inPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an input prototype ", colI);
//                outPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an output prototype ", colO);
//                menus.add(inPrototypeMenu);
//                menus.add(outPrototypeMenu);
//            }
//        }
//        //Update tasks
//        if (!TasksUI.getInstance().getAll().isEmpty()) {
//            menus.remove(taskMenu);
//            Collection<JMenuItem> colTask = new ArrayList<JMenuItem>();
//            for (IEntityUI c : TasksUI.getInstance().getAll()) {
//                JMenuItem it = new JMenuItem(c.getName() + " :: " + c.getType().getSimpleName());
//                it.addActionListener(new AddTaskAction(scene,
//                        capsuleView, (TaskUI) c));
//                colTask.add(it);
//            }
//            taskMenu = PopupMenuProviderFactory.addSubMenu("Encapsulate a task ", colTask);
//            menus.add(taskMenu);
//        }
//        itIS.setEnabled(!capsuleView.getCapsuleModel().isStartingCapsule());
//
//        retu


//
//import java.awt.Point;
//import java.util.ArrayList;
//import java.util.Collection;
//import javax.swing.JMenu;
//import javax.swing.JMenuItem;
//import javax.swing.JPopupMenu;
//import org.netbeans.api.visual.widget.Widget;
//import org.openmole.ui.ide.commons.IOType;
//import org.openmole.ui.ide.workflow.action.AddExistingPrototypeAction;
//import org.openmole.ui.ide.workflow.action.AddInputSlotAction;
//import org.openmole.ui.ide.workflow.action.AddTaskAction;
//import org.openmole.ui.ide.workflow.action.DefineMoleStartAction;
//import org.openmole.ui.ide.workflow.action.RemoveCapsuleAction;
//import org.openmole.ui.ide.workflow.implementation.MoleScene;
//import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI;
//import org.openmole.ui.ide.workflow.implementation.IEntityUI;
//import org.openmole.ui.ide.workflow.implementation.PrototypesUI;
//import org.openmole.ui.ide.workflow.implementation.TaskUI;
//import org.openmole.ui.ide.workflow.implementation.TasksUI;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
// */
//public class CapsuleMenuProvider extends GenericMenuProvider {
//
//    private JMenu inPrototypeMenu;
//    private JMenu outPrototypeMenu;
//    private JMenu taskMenu;
//    private CapsuleViewUI capsuleView = null;
//    private boolean encapsulated = false;
//    JMenuItem itIS = new JMenuItem();
//    MoleScene scene;
//
//    @Override
//    public JPopupMenu getPopupMenu(Widget widget, Point point) {
//        //Update prototypes
//        if (encapsulated) {
//            Collection<JMenuItem> colI = fillPrototypeMenu(IOType.INPUT);
//            Collection<JMenuItem> colO = fillPrototypeMenu(IOType.OUTPUT);
//            if (!colI.isEmpty()) {
//                menus.remove(inPrototypeMenu);
//                menus.remove(outPrototypeMenu);
//                inPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an input prototype ", colI);
//                outPrototypeMenu = PopupMenuProviderFactory.addSubMenu("Add an output prototype ", colO);
//                menus.add(inPrototypeMenu);
//                menus.add(outPrototypeMenu);
//            }
//        }
//        //Update tasks
//        if (!TasksUI.getInstance().getAll().isEmpty()) {
//            menus.remove(taskMenu);
//            Collection<JMenuItem> colTask = new ArrayList<JMenuItem>();
//            for (IEntityUI c : TasksUI.getInstance().getAll()) {
//                JMenuItem it = new JMenuItem(c.getName() + " :: " + c.getType().getSimpleName());
//                it.addActionListener(new AddTaskAction(scene,
//                        capsuleView, (TaskUI) c));
//                colTask.add(it);
//            }
//            taskMenu = PopupMenuProviderFactory.addSubMenu("Encapsulate a task ", colTask);
//            menus.add(taskMenu);
//        }
//        itIS.setEnabled(!capsuleView.getCapsuleModel().isStartingCapsule());
//
//        return super.getPopupMenu(widget, point);
//    }
//
//    public CapsuleMenuProvider(MoleScene scene,
//            CapsuleViewUI capsuleView) {
//        super();
//        this.capsuleView = capsuleView;
//        this.scene = scene;
//
//        itIS = new JMenuItem("Add an input slot");
//        itIS.addActionListener(new AddInputSlotAction(capsuleView));
//        JMenuItem itR = new JMenuItem("Remove capsule");
//        itR.addActionListener(new RemoveCapsuleAction(scene,capsuleView));
//        JMenuItem itStart = new JMenuItem("Define as starting capsule");
//        itStart.addActionListener(new DefineMoleStartAction(scene, capsuleView));
//
//        items.add(itIS);
//        items.add(itR);
//        items.add(itStart);
//
//    }
//
//    public void addTaskMenus() {
//        encapsulated = true;
//    }
//
//    public Collection<JMenuItem> fillPrototypeMenu(IOType type) {
//        Collection<JMenuItem> prototypeCol = new ArrayList<JMenuItem>();
//        for (IEntityUI p : PrototypesUI.getInstance().getAll()) {
//            JMenuItem it = new JMenuItem(p.getName() + " :: " + p.getType().getSimpleName());
//            it.addActionListener(new AddExistingPrototypeAction(p, capsuleView, type));
//            prototypeCol.add(it);
//        }
//        return prototypeCol;
//    }
//}
