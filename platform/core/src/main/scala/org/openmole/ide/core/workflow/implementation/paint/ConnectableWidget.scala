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

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Color
import java.awt.Container
import java.awt.Graphics2D
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.workflow.implementation.CapsuleModelUI
import org.openmole.ide.core.workflow.implementation.CapsuleViewUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.workflow.model.ICapsuleModelUI
import scala.collection.mutable.HashSet

class ConnectableWidget(scene: MoleScene, val capsuleView: CapsuleViewUI) extends MyWidget(scene, capsuleView.capsuleModel){

  var islots= HashSet.empty[ISlotWidget]
  val oslot= new OSlotWidget(scene,capsuleView)
  
  addChild(oslot)
  createActions(scene.MOVE).addAction(ActionFactory.createMoveAction)
  
  def setDetailedView= {
    setWidthHint
    oslot.setDetailedView(taskWidth)
  }
  
  def addInputSlot(iw: ISlotWidget) {
    islots.add(iw)
    addChild(iw)
    scene.validate
  }
    
  def clearInputSlots= {
    islots.foreach(removeChild(_))
    islots.clear
  }
 
  override def paintWidget= {
    super.paintWidget
    val graphics = getGraphics.asInstanceOf[Graphics2D]

   // graphics.setColor(taskUI.factory.borderColor)
    if (capsuleView.capsuleModel.taskUI.isDefined) {
      graphics.drawLine(taskWidth / 2,
                        Constants.TASK_TITLE_HEIGHT,
                        taskWidth / 2,
                        widgetArea.height - 3)

      graphics.setColor(new Color(0, 0, 0))
      var x = taskWidth / 2 + 9
      
      var i=0
      (capsuleView.capsuleModel.taskUI.get.prototypesIn.toList:::capsuleView.capsuleModel.taskUI.get.prototypesOut.toList).foreach(p=> {
          if (i > capsuleView.capsuleModel.taskUI.get.prototypesIn.size) i= 0
          var st = p.panelUIData.name
          if (st.length> 10) st = st.substring(0, 8).concat("...")
          val h= 35 + i * 22
          graphics.drawImage(Constants.typeImageMap(p.factoryUI.coreClass.getSimpleName),
                             x - taskWidth / 2, h - 13,
                             new Container)
          if (MoleScenesManager.detailedView) graphics.drawString(st, x - taskWidth / 2 + 24, h)
          x += taskWidth / 2 - 1
          i+= 1
        })

      val newH= Math.max(capsuleView.capsuleModel.taskUI.get.prototypesIn.size, capsuleView.capsuleModel.taskUI.get.prototypesOut.size) * 22 + 45
      val delta= bodyArea.height - newH
      if (delta < 0) {
        bodyArea.setSize(bodyArea.width, newH)
        enlargeWidgetArea(0, -delta)
      }
    }
    revalidate
  }
  
}

//
//
//ConnectableWidget extends MyWidget {
//
//  private IGenericTaskModelUI<IGenericTask> taskModel = TaskModelUI.EMPTY_TASK_MODEL;
//  List<ISlotWidget> islots = new ArrayList<ISlotWidget>();
//  OSlotWidget oslot;
//
//  //private WidgetAction mouseHoverAction = ActionFactory.createHoverAction(new ImageHoverProvider());
//  public ConnectableWidget(MoleScene scene,
//                           ICapsuleModelUI capsuleModel,
//                           Color backgroundCol,
//                           Color borderCol,
//                           Image img) {
//    super(scene,
//          backgroundCol,
//          borderCol,
//          img);
//    this.borderCol = borderCol;
//  }
//
//  public ConnectableWidget(MoleScene scene,
//                           ICapsuleModelUI capsuleModel,
//                           Color backgroundCol,
//                           Color borderCol,
//                           Image backgroundImaqe,
//                           IGenericTaskModelUI taskModelUI) {
//    this(scene, capsuleModel, backgroundCol, borderCol, backgroundImaqe);
//    this.taskModel = taskModelUI;
//    createActions(MoleScene.MOVE).addAction(ActionFactory.createMoveAction());
//  }
//
//  public void setTaskModel(IGenericTaskModelUI<IGenericTask> taskModelUI) {
//    this.taskModel = taskModelUI;
//  }
//
//  public void setDetailedView() {
//    setWidthHint();
//    oslot.setDetailedView(taskWidth);
//  }
//
//  public void addInputSlot(ISlotWidget iw) {
//    islots.add(iw);
//    addChild(iw);
//  }
//
//  public List<ISlotWidget> getIslots() {
//    return islots;
//  }
//
//  public void addOutputSlot(OSlotWidget ow) {
//    addChild(ow);
//    oslot = ow;
//  }
//
//  public void clearInputSlots() {
//    for (ImageWidget iw : islots) {
//      removeChild(iw);
//    }
//    islots.clear();
//  }
//
//  @Override
//  protected void paintWidget() {
//    super.paintWidget();
//    Graphics2D graphics = getGraphics();
//
//    graphics.setColor(borderCol);
//    BasicStroke stroke = new BasicStroke(1.3f, 1, 1);
//    graphics.draw(stroke.createStrokedShape(bodyArea));
//
//    if (taskModel != TaskModelUI.EMPTY_TASK_MODEL) {
//      graphics.drawLine(taskWidth / 2,
//                        Constants.TASK_TITLE_HEIGHT,
//                        taskWidth / 2,
//                        widgetArea.height - 3);
//
//      graphics.setColor(new Color(0, 0, 0));
//      int x = taskWidth / 2 + 9;
//
//      List<Set<PrototypeUI>> li = new ArrayList<Set<PrototypeUI>>();
//      li.add(taskModel.getPrototypesIn());
//      li.add(taskModel.getPrototypesOut());
//      int h = 0;
//      for (Set<PrototypeUI> protoIO : li) {
//        int i = 0;
//        for (PrototypeUI proto : protoIO) {
//          String st = proto.getName();
//          if (st.length() > 10) {
//            st = st.substring(0, 8).concat("...");
//          }
//          h = 35 + i * 22;
//          graphics.drawImage(Constants.getInstance().getTypeImage(proto.getType().getSimpleName()),
//                             x - taskWidth / 2, h - 13,
//                             new Container());
//          AttributedString as = new AttributedString(st);
//          if (MoleScenesManager.getInstance().isDetailedView()) {
//            graphics.drawString(as.getIterator(), x - taskWidth / 2 + 24, h);
//          }
//          i++;
//        }
//        x += taskWidth / 2 - 1;
//      }
//
//      int newH = Math.max(taskModel.getPrototypesIn().size(), taskModel.getPrototypesOut().size()) * 22 + 45;
//      int delta = bodyArea.height - newH;
//      if (delta < 0) {
//        bodyArea.setSize(bodyArea.width, newH);
//        enlargeWidgetArea(0, -delta);
//      }
//    }
//    revalidate();
//  }