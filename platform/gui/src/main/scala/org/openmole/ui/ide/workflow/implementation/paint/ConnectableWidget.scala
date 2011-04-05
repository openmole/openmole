/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation.paint

import java.awt.Color
import java.awt.BasicStroke
import java.awt.Container
import java.awt.Graphics2D
import org.openmole.ui.ide.control.MoleScenesManager
import org.openmole.ui.ide.workflow.implementation.CapsuleViewUI
import org.openmole.ui.ide.workflow.implementation.MoleScene
import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI
import org.openmole.ui.ide.workflow.model.IObjectViewUI
import org.netbeans.api.visual.action.ActionFactory
import org.openmole.ui.ide.commons.ApplicationCustomize
import scala.collection.mutable.HashSet

class ConnectableWidget(scene: MoleScene, objectView: CapsuleViewUI, val taskModel: Option[IGenericTaskModelUI]= None) extends MyWidget(scene, objectView){

  var islots= HashSet.empty[ISlotWidget]
  val oslot= new OSlotWidget(scene,objectView)
  addChild(oslot)
  createActions(scene.MOVE).addAction(ActionFactory.createMoveAction)
  
  def setDetailedView= {
    setWidthHint
    oslot.setDetailedView(taskWidth)
  }
  
  def addInputSlot(iw: ISlotWidget) {
    islots.add(iw)
    addChild(iw)
  }
  
  def clearInputSlots= {
    islots.foreach(removeChild(_))
    islots.clear
  }
  
  override def paintWidget= {
    super.paintWidget
    val graphics = getGraphics

    graphics.setColor(objectView.borderColor)
    val stroke = new BasicStroke(1.3f, 1, 1)
    graphics.draw(stroke.createStrokedShape(bodyArea))

    if (taskModel.isDefined) {
      graphics.drawLine(taskWidth / 2,
                        ApplicationCustomize.TASK_TITLE_HEIGHT,
                        taskWidth / 2,
                        widgetArea.height - 3)

      graphics.setColor(new Color(0, 0, 0))
      var x = taskWidth / 2 + 9
      
      var i=0
      (taskModel.get.prototypesIn.toList:::taskModel.get.prototypesOut.toList).foreach(p=> {
          if (i >= taskModel.get.prototypesIn.size) i= 0
          var st = p.name
          if (st.length> 10) st = st.substring(0, 8).concat("...")
          val h= 35 + i * 22
          graphics.drawImage(ApplicationCustomize.typeImageMap(p.entityType.getSimpleName),
                             x - taskWidth / 2, h - 13,
                             new Container)
          if (MoleScenesManager.detailedView) graphics.drawString(st, x - taskWidth / 2 + 24, h)
          x += taskWidth / 2 - 1
          i+= 1
        })
      

      val newH= Math.max(taskModel.get.prototypesIn.size, taskModel.get.prototypesOut.size) * 22 + 45
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
//                        ApplicationCustomize.TASK_TITLE_HEIGHT,
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
//          graphics.drawImage(ApplicationCustomize.getInstance().getTypeImage(proto.getType().getSimpleName()),
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