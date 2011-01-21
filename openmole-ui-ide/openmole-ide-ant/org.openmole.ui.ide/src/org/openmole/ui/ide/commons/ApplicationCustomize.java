/*
 *  Copyright (C) 2010 leclaire
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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
package org.openmole.ui.ide.commons;

import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.LinkedHashMap;
import org.openide.util.ImageUtilities;
import org.openmole.ui.ide.workflow.implementation.Preferences;
import org.openmole.ui.ide.workflow.implementation.PrototypeUI;
import org.openmole.ui.ide.workflow.implementation.TaskUI;
import org.openmole.ui.ide.workflow.model.ICapsuleModelUI;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class ApplicationCustomize {

    private static ApplicationCustomize instance = null;
    private LinkedHashMap<String, Color> colorMap = new LinkedHashMap<String, Color>();
    private LinkedHashMap<String, Image> typeImageMap = new LinkedHashMap<String, Image>();
    public static final String TASK_HEADER_BACKGROUND_COLOR = "TASK_HEADER_BACKGROUND_COLOR";
    public static final String TASK_SELECTION_COLOR = "TASK_SELECTION_COLOR";
    public static final String CONDITION_LABEL_BACKGROUND_COLOR = "CONDITION_LABEL_BACKGROUND_COLOR";
    public static final String CONDITION_LABEL_BORDER_COLOR = "CONDITION_LABEL_BORDER_COLOR";
    public static final int SCREEN_WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
    public static final int SCREEN_HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
    public static final int PANEL_WIDTH = (int) (SCREEN_WIDTH * 0.8);
    public static final int PANEL_HEIGHT = (int) (SCREEN_HEIGHT * 0.8);
    public static final int EXPANDED_TASK_CONTAINER_WIDTH = 200;
    public static final int TASK_CONTAINER_WIDTH = 80;
    public static final int TASK_CONTAINER_HEIGHT = 100;
    public static final int TASK_TITLE_WIDTH = TASK_CONTAINER_WIDTH;
    public static final int TASK_TITLE_HEIGHT = 20;
    public static final int TASK_IMAGE_HEIGHT = TASK_CONTAINER_HEIGHT - TASK_TITLE_HEIGHT - 20;
    public static final int TASK_IMAGE_WIDTH = 70;
    public static final int TASK_IMAGE_HEIGHT_OFFSET = TASK_TITLE_HEIGHT + 10;
    public static final int TASK_IMAGE_WIDTH_OFFSET = (TASK_CONTAINER_WIDTH - TASK_IMAGE_WIDTH) / 2;
    public static final int EXPANDED_TASK_IMAGE_WIDTH_OFFSET = (EXPANDED_TASK_CONTAINER_WIDTH - TASK_IMAGE_WIDTH) / 2;
    public static final int DATA_TABLE_X_OFFSET = (int) (2 + TASK_CONTAINER_WIDTH * 0.1);
    public static final int DATA_TABLE_Y_OFFSET = (int) (TASK_CONTAINER_HEIGHT * 0.1 - 2);
    public static final Image IMAGE_START_SLOT = ImageUtilities.loadImage("resources/img/startSlot.png");
    public static final Image IMAGE_INPUT_SLOT = ImageUtilities.loadImage("resources/img/inputSlot.png");
    public static final Image IMAGE_OUTPUT_SLOT = ImageUtilities.loadImage("resources/img/outputSlot.png");
    public static final Image IMAGE_TRANSITIONS = ImageUtilities.loadImage("resources/img/transitions.png");
    public static final int NB_MAX_SLOTS = 5;
    public static final DataFlavor PROTOTYPE_DATA_FLAVOR = new DataFlavor(PrototypeUI.class, "Prototypes");
    public static final DataFlavor TASK_DATA_FLAVOR = new DataFlavor(TaskUI.class, "Tasks");
    public static final DataFlavor TASK_CAPSULE_DATA_FLAVOR = new DataFlavor(ICapsuleModelUI.class, "Task capsules");
    public static final String TASK_DEFAULT_PROPERTIES = "src/resources/task/default";
    public static final Class CORE_CAPSULE_CLASS = org.openmole.core.implementation.capsule.Capsule.class;

    public ApplicationCustomize() {
        setDefaultColors();
        setDefaultTypeImages();
    }

    private void setDefaultTypeImages() {
        for (Class c : Preferences.getInstance().getPrototypeTypeClasses()) {
            typeImageMap.put(c.getSimpleName(), ImageUtilities.loadImage("resources/img/" + c.getSimpleName() + ".png"));
        }
    }

    public Image getTypeImage(String type) {
        return typeImageMap.get(type);
    }

    private void setDefaultColors() {
        colorMap.put(TASK_HEADER_BACKGROUND_COLOR, new Color(68, 120, 33));
        colorMap.put(TASK_SELECTION_COLOR, new Color(255, 100, 0));
        colorMap.put(CONDITION_LABEL_BACKGROUND_COLOR, new Color(255, 238, 170));
        colorMap.put(CONDITION_LABEL_BORDER_COLOR, new Color(230, 180, 0));
    }

    public Color getColor(String str) {
        return colorMap.get(str);
    }

    public static ApplicationCustomize getInstance() {
        if (instance == null) {
            instance = new ApplicationCustomize();
        }
        return instance;
    }
}
