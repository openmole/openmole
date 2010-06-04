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
package org.openmole.ui.commons;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.LinkedHashMap;
import java.util.prefs.Preferences;
import org.openide.util.ImageUtilities;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.ui.workflow.model.ITaskCapsuleModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class ApplicationCustomize {

    private static ApplicationCustomize instance = null;
    private LinkedHashMap<String, Color> colorMap = new LinkedHashMap<String, Color>();

    public static final String TASK_HEADER_BACKGROUND_COLOR = "TASK_HEADER_BACKGROUND_COLOR";
    public static final String TASK_SELECTION_COLOR = "TASK_SELECTION_COLOR";

   
    public static final String TABLE_HEADER_COLOR = "TABLE_HEADER_COLOR";
    public static final String TABLE_ROW_COLOR = "TABLE_ROW_COLOR";

    
    public static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    public static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    public static final int PANEL_WIDTH =  (int) (SCREEN_WIDTH * 0.8);
    public static final int PANEL_HEIGHT = (int) (SCREEN_HEIGHT * 0.8);

    public static final int TASK_CONTAINER_WIDTH = 80;
    public static final int TASK_CONTAINER_HEIGHT = 100;
    public static final int TASK_TITLE_WIDTH = TASK_CONTAINER_WIDTH;
    public static final int TASK_TITLE_HEIGHT = 20;
    public static final int TASK_IMAGE_HEIGHT = TASK_CONTAINER_HEIGHT - TASK_TITLE_HEIGHT -20;
    public static final int TASK_IMAGE_WIDTH = TASK_CONTAINER_WIDTH - 10;
    public static final int TASK_IMAGE_HEIGHT_OFFSET = TASK_TITLE_HEIGHT + 10;
    public static final int TASK_IMAGE_WIDTH_OFFSET = 5;
    public static final int DATA_TABLE_X_OFFSET = (int) (2 + TASK_CONTAINER_WIDTH * 0.1);
    public static final int DATA_TABLE_Y_OFFSET = (int) (TASK_CONTAINER_HEIGHT * 0.1 - 2);

    public static final Image IMAGE_COLLAPSE = ImageUtilities.loadImage("resources/img/collapse.png");
    public static final Image IMAGE_EXPAND = ImageUtilities.loadImage("resources/img/expand.png");
    public static final Image IMAGE_INPUT_ARROW = ImageUtilities.loadImage("resources/img/inputArrow.png");
    public static final Image IMAGE_OUTPUT_ARROW = ImageUtilities.loadImage("resources/img/outputArrow.png");
    public static final Image IMAGE_INPUT_SLOT = ImageUtilities.loadImage("resources/img/inputSlot.png");
    public static final Image IMAGE_OUTPUT_SLOT = ImageUtilities.loadImage("resources/img/outputSlot.png");

    public static final int NB_MAX_SLOTS = 5;

    public static final DataFlavor PROTOTYPE_DATA_FLAVOR = new DataFlavor( Class.class, "Prototypes" );
    public static final DataFlavor TASK_DATA_FLAVOR = new DataFlavor( IGenericTask.class, "Tasks" );
    public static final DataFlavor TASK_CAPSULE_DATA_FLAVOR = new DataFlavor( ITaskCapsuleModelUI.class, "Task capsules" );

    public ApplicationCustomize() {
        setDefaultColors();
    }


    private void loadFromPreferences(Preferences prefs) {
        // TO DO: Implements preference mechanism
        setDefaultColors();
    }

    private void setDefaultColors() {
        colorMap.put(TASK_HEADER_BACKGROUND_COLOR, new Color(68,120,33));
        colorMap.put(TASK_SELECTION_COLOR, new Color(255, 100, 0));
        colorMap.put(TABLE_HEADER_COLOR, new Color(227,222,219,150));
        colorMap.put(TABLE_ROW_COLOR, new Color(255,238,170,150));
    }

    public Color getColor(String str){
        return colorMap.get(str);
    }

    public static ApplicationCustomize getInstance() {
        if (instance == null) {
            instance = new ApplicationCustomize();
        }
        return instance;
    }
}
