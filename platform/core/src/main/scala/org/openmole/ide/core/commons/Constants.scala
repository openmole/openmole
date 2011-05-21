/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.commons

import java.awt.Color
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import org.openide.util.ImageUtilities
import scala.collection.immutable.HashMap
import org.openmole.ide.core.workflow.implementation.EntityUI
import org.openmole.ide.core.palette.PaletteElementFactory
import org.openmole.ide.core.workflow.implementation.TaskUI

object Constants{
  
  val TASK_HEADER_BACKGROUND_COLOR= new Color(68, 120, 33)
  val TASK_SELECTION_COLOR= new Color(255, 100, 0)
  val CONDITION_LABEL_BACKGROUND_COLOR= new Color(255, 238, 170)
  val CONDITION_LABEL_BORDER_COLOR= new Color(230, 180, 0)
  val DEFAULT_BACKGROUND_TASK_COLOR= new Color(255,255,255)
  val DEFAULT_BORDER_TASK_COLOR= new Color(0,0,0)
  
  val SCREEN_WIDTH= Toolkit.getDefaultToolkit.getScreenSize.width
  val SCREEN_HEIGHT= Toolkit.getDefaultToolkit.getScreenSize.height
  val PANEL_WIDTH= SCREEN_WIDTH* 0.8
  val PANEL_HEIGHT= SCREEN_HEIGHT* 0.8
  val EXPANDED_TASK_CONTAINER_WIDTH= 200
  val TASK_CONTAINER_WIDTH= 80
  val TASK_CONTAINER_HEIGHT= 100
  val TASK_TITLE_WIDTH = TASK_CONTAINER_WIDTH
  val TASK_TITLE_HEIGHT= 20
  val TASK_IMAGE_HEIGHT = TASK_CONTAINER_HEIGHT - TASK_TITLE_HEIGHT - 20
  val TASK_IMAGE_WIDTH= 70
  val TASK_IMAGE_HEIGHT_OFFSET= TASK_TITLE_HEIGHT + 10
  val TASK_IMAGE_WIDTH_OFFSET= (TASK_CONTAINER_WIDTH - TASK_IMAGE_WIDTH) / 2
  val EXPANDED_TASK_IMAGE_WIDTH_OFFSET= (EXPANDED_TASK_CONTAINER_WIDTH - TASK_IMAGE_WIDTH) / 2
  val NB_MAX_SLOTS= 5
    
  val TASK = "TASK"
  val PROTOTYPE = "PROTOTYPE"
  val SAMPLING = "SAMPLING"
  val ENVIRONMENT = "ENVIRONMENT"
  
  val ENTITY_DATA_FLAVOR= new DataFlavor(classOf[EntityUI], PROTOTYPE)
  val TASK_DATA_FLAVOR= new DataFlavor(classOf[TaskUI], TASK)

 // def simpleEntityName(entityName: String) = entityName.split('_')(0)
}
//
//
// private static Constants instance = null;
//    
//    public static final String RESOURCES_PATH = System.getProperty("user.dir")+"/resources/";
//   // public static final String RESOURCES_PATH = "src/main/resources/";
//    private LinkedHashMap<String, Color> colorMap = new LinkedHashMap<String, Color>();
//    private LinkedHashMap<String, Image> typeImageMap = new LinkedHashMap<String, Image>();
//    public static final String TASK_HEADER_BACKGROUND_COLOR = "TASK_HEADER_BACKGROUND_COLOR";
//    public static final String TASK_SELECTION_COLOR = "TASK_SELECTION_COLOR";
//    public static final String CONDITION_LABEL_BACKGROUND_COLOR = "CONDITION_LABEL_BACKGROUND_COLOR";
//    public static final String CONDITION_LABEL_BORDER_COLOR = "CONDITION_LABEL_BORDER_COLOR";
//    public static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
//    public static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
//    public static final int PANEL_WIDTH = (int) (SCREEN_WIDTH * 0.8);
//    public static final int PANEL_HEIGHT = (int) (SCREEN_HEIGHT * 0.8);
//    public static final int EXPANDED_TASK_CONTAINER_WIDTH = 200;
//    public static final int TASK_CONTAINER_WIDTH = 80;
//    public static final int TASK_CONTAINER_HEIGHT = 100;
//    public static final int TASK_TITLE_WIDTH = TASK_CONTAINER_WIDTH;
//    public static final int TASK_TITLE_HEIGHT = 20;
//    public static final int TASK_IMAGE_HEIGHT = TASK_CONTAINER_HEIGHT - TASK_TITLE_HEIGHT - 20;
//    public static final int TASK_IMAGE_WIDTH = 70;
//    public static final int TASK_IMAGE_HEIGHT_OFFSET = TASK_TITLE_HEIGHT + 10;
//    public static final int TASK_IMAGE_WIDTH_OFFSET = (TASK_CONTAINER_WIDTH - TASK_IMAGE_WIDTH) / 2;
//    public static final int EXPANDED_TASK_IMAGE_WIDTH_OFFSET = (EXPANDED_TASK_CONTAINER_WIDTH - TASK_IMAGE_WIDTH) / 2;
//    public static final int DATA_TABLE_X_OFFSET = (int) (2 + TASK_CONTAINER_WIDTH * 0.1);
//    public static final int DATA_TABLE_Y_OFFSET = (int) (TASK_CONTAINER_HEIGHT * 0.1 - 2);
//    
//    public static final Image IMAGE_START_SLOT = ImageUtilities.loadImage("img/startSlot.png");
//    public static final Image IMAGE_INPUT_SLOT = ImageUtilities.loadImage("img/inputSlot.png");
//    public static final Image IMAGE_OUTPUT_SLOT = ImageUtilities.loadImage("img/outputSlot.png");
//    public static final int NB_MAX_SLOTS = 5;
//    
//    public static final DataFlavor PROTOTYPE_DATA_FLAVOR = new DataFlavor(PrototypeUI.class, "Prototypes");
//    public static final DataFlavor TASK_DATA_FLAVOR = new DataFlavor(TaskUI.class, "Tasks");
//    public static final DataFlavor SAMPLING_DATA_FLAVOR = new DataFlavor(SamplingUI.class, "Samplings");
//  //  public static final String TASK_DEFAULT_PROPERTIES = RESOURCES_PATH + "task/default";
//    public static final String TASK_DEFAULT_PROPERTIES = "task/default";
//
//    public Constants() {
//        setDefaultColors();
//        setDefaultTypeImages();
//    }
//    
//    private void setDefaultTypeImages() {
//        for (Class c : JavaConversions.asJavaIterable(Preferences.getPrototypeTypeClasses())) {
//            typeImageMap.put(c.getSimpleName(), ImageUtilities.loadImage("img/" + c.getSimpleName() + ".png"));
//        }
//    }
//
//    public Image getTypeImage(String type) {
//        return typeImageMap.get(type);
//    }
//
//    private void setDefaultColors() {
//        colorMap.put(TASK_HEADER_BACKGROUND_COLOR, new Color(68, 120, 33));
//        colorMap.put(TASK_SELECTION_COLOR, new Color(255, 100, 0));
//        colorMap.put(CONDITION_LABEL_BACKGROUND_COLOR, new Color(255, 238, 170));
//        colorMap.put(CONDITION_LABEL_BORDER_COLOR, new Color(230, 180, 0));
//    }
//
//    public Color getColor(String str) {
//        return colorMap.get(str);
//    }
//
//    public static Constants getInstance() {
//        if (instance == null) {
//            instance = new Constants();
//        }
//        return instance;
//    }