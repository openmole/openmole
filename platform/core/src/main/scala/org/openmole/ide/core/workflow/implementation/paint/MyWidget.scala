/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation.paint

import java.awt.Color
import java.awt.Image
import java.awt.Rectangle
import java.awt.Container
import java.awt.Graphics2D
import org.netbeans.api.visual.widget._
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.model.ICapsuleModelUI
import org.openide.util.ImageUtilities
import java.awt.BasicStroke

class MyWidget(scene: MoleScene,capsuleModel: ICapsuleModelUI,var title: Option[String]= Some("")) extends Widget(scene) {

  var taskWidth= Constants.TASK_CONTAINER_WIDTH
  var taskImageOffset= Constants.TASK_IMAGE_WIDTH_OFFSET
  val bodyArea = new Rectangle
  val widgetArea= new Rectangle
  var titleArea = new Rectangle
  setWidthHint
  //def this(scene: MoleScene, w: MyWidget,title: String)= this(scene,w.objectView,Some(title))
  
  def widgetWidth= widgetArea.width
  
  
  def setWidthHint= {
    if (MoleScenesManager.detailedView) {
      taskWidth = Constants.EXPANDED_TASK_CONTAINER_WIDTH
      taskImageOffset = Constants.EXPANDED_TASK_IMAGE_WIDTH_OFFSET
    }
    else {
      taskWidth = Constants.TASK_CONTAINER_WIDTH
      taskImageOffset = Constants.TASK_IMAGE_WIDTH_OFFSET
    }
    bodyArea.setBounds(new Rectangle(0, 0,taskWidth,Constants.TASK_CONTAINER_HEIGHT))
    widgetArea.setBounds(new Rectangle(-12, -1,taskWidth + 24,Constants.TASK_CONTAINER_HEIGHT + 2))
    titleArea.setBounds(new Rectangle(0, 0,taskWidth,Constants.TASK_TITLE_HEIGHT))
    setPreferredBounds(widgetArea)
    revalidate
    repaint
  }
  
  def enlargeWidgetArea(y: Int,height: Int) {
    widgetArea.height += height
    widgetArea.y -= y
  }
  
  override def paintWidget= {
    val graphics= getGraphics.asInstanceOf[Graphics2D]
    graphics.setColor(if(capsuleModel.taskUI.isDefined) capsuleModel.taskUI.get.factoryUI.backgroundColor else new Color(204,204,204,128))
    graphics.fill(bodyArea)
    graphics.setColor(if(capsuleModel.taskUI.isDefined) capsuleModel.taskUI.get.factoryUI.borderColor else new Color(204,204,204))

    val stroke = new BasicStroke(1.3f, 1, 1)
    graphics.draw(stroke.createStrokedShape(bodyArea))
    
    if (title.isDefined) {
      graphics.fill(titleArea)
      graphics.setColor(Color.WHITE)
      graphics.drawString(title.get, 10, 15)
    }

   /* if(capsuleModel.taskUI.isDefined) graphics.drawImage(ImageUtilities.loadImage(capsuleModel.taskUI.get.factory.imagePath),
                                                         taskImageOffset,
                                                         Constants.TASK_IMAGE_HEIGHT_OFFSET,
                                                         Constants.TASK_IMAGE_WIDTH,
                                                         Constants.TASK_IMAGE_HEIGHT,
                                                         capsuleModel.taskUI.get.factory.backgroundColor,
                                                         new Container)*/
  }

  def addTitle(titleString: String)= {
    titleArea = new Rectangle(0, 0,taskWidth,Constants.TASK_TITLE_HEIGHT)
    title = Some(titleString)
    setPreferredBounds(widgetArea)
  }
  
}

//
//public class MyWidget extends Widget {
//  private Color backgroundCol;
//  protected Color borderCol;
//  private Image backgroundImaqe;
//  protected Rectangle bodyArea = new Rectangle();
//  protected Rectangle widgetArea = new Rectangle();
//  private Rectangle titleArea = new Rectangle();
//  private boolean title = false;
//  private boolean image = false;
//  private String titleString;
//  protected int taskWidth = Constants.TASK_CONTAINER_WIDTH;
//  protected int taskImageOffset = Constants.TASK_IMAGE_WIDTH_OFFSET;
//  protected MoleScene scene;
//
//  public MyWidget(MoleScene scene,
//                  Color col) {
//    super(scene);
//    this.backgroundCol = col;
//    this.scene = scene;
//    setWidthHint();
//  }
//
//  public void setWidthHint() {
//    if (MoleScenesManager.getInstance().isDetailedView()) {
//      taskWidth = Constants.EXPANDED_TASK_CONTAINER_WIDTH;
//      taskImageOffset = Constants.EXPANDED_TASK_IMAGE_WIDTH_OFFSET;
//    }
//    else {
//      taskWidth = Constants.TASK_CONTAINER_WIDTH;
//      taskImageOffset = Constants.TASK_IMAGE_WIDTH_OFFSET;
//    }
//
//    Rectangle bodyrect = new Rectangle(0, 0,
//                                       taskWidth,
//                                       Constants.TASK_CONTAINER_HEIGHT);
//
//    Rectangle widgetrect = new Rectangle(-12, -1,
//                                         taskWidth + 24,
//                                         Constants.TASK_CONTAINER_HEIGHT + 2);
//    bodyArea.setBounds(bodyrect);
//    widgetArea.setBounds(widgetrect);
//    titleArea.setBounds(new Rectangle(0, 0,
//                                      taskWidth,
//                                      Constants.TASK_TITLE_HEIGHT));
//    setPreferredBounds(widgetArea);
//    revalidate();
//    repaint();
//  }
//
//  public MyWidget(MoleScene scene,
//                  Color backgroundCol,
//                  Color borderCol) {
//    this(scene, backgroundCol);
//    this.borderCol = borderCol;
//  }
//
//  public MyWidget(MoleScene scene,
//                  Color backgroundCol,
//                  Color borderCol,
//                  Image backgroundImaqe) {
//    this(scene, backgroundCol, borderCol);
//    this.image = true;
//    this.backgroundImaqe = backgroundImaqe;
//  }
//
//  protected void enlargeWidgetArea(int y,
//                                   int height) {
//    widgetArea.height += height;
//    widgetArea.y -= y;
//  }
//
//  public void setBackgroundCol(Color backgroundCol) {
//    this.backgroundCol = backgroundCol;
//  }
//
//  public void setBackgroundImaqe(Image backgroundImaqe) {
//    this.backgroundImaqe = backgroundImaqe;
//  }
//
//  public void setBorderCol(Color borderCol) {
//    this.borderCol = borderCol;
//  }
//
//  public void setTitle(String title) {
//    this.titleString = title;
//  }
//
//  public String getTitleString() {
//    return titleString;
//  }
//
//  public int getWidgetWidth(){
//    return widgetArea.width;
//  }
//    
//  public void addTitle(String titleString) {
//    titleArea = new Rectangle(0, 0,
//                              taskWidth,
//                              Constants.TASK_TITLE_HEIGHT);
//
//    this.title = true;
//    this.titleString = titleString;
//
//    setPreferredBounds(widgetArea);
//  }
//
//  @Override
//  protected void paintWidget() {
//    Graphics2D graphics = getGraphics();
//    graphics.setColor(backgroundCol);
//    graphics.fill(bodyArea);
//
//    graphics.setColor(borderCol);
//
//    if (title) {
//      graphics.fill(titleArea);
//      graphics.setColor(Color.WHITE);
//      graphics.drawString(titleString, 10, 15);
//    }
//
//    if (image) {
//      graphics.drawImage(backgroundImaqe,
//                         taskImageOffset,
//                         Constants.TASK_IMAGE_HEIGHT_OFFSET,
//                         Constants.TASK_IMAGE_WIDTH,
//                         Constants.TASK_IMAGE_HEIGHT,
//                         backgroundCol,
//                         new Container());
//    }
//  }
//}
