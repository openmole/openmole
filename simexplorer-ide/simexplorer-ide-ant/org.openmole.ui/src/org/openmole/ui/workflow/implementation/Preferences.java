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

package org.openmole.ui.workflow.implementation;

import java.awt.Color;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openmole.core.workflow.implementation.capsule.ExplorationTaskCapsule;
import org.openmole.core.workflow.implementation.capsule.TaskCapsule;
import org.openmole.ui.commons.ApplicationCustomize;
import org.openmole.ui.workflow.model.IObjectModelUI;
import org.openmole.core.workflow.implementation.task.ExplorationTask;
import org.openmole.plugin.task.groovytask.GroovyTask;
import org.openmole.core.workflow.methods.task.JavaTask;
import org.openmole.core.workflow.model.capsule.IGenericTaskCapsule;
import org.openmole.commons.tools.service.HierarchicalRegistry;
import org.openmole.ui.workflow.model.ICapsuleModelUI;
import org.openmole.ui.workflow.model.IGenericTaskModelUI;

/**
 *
 * @author mathieu
 */
public class Preferences {
    private static Preferences instance = null;
    private ApplicationCustomize app = ApplicationCustomize.getInstance();

    private HierarchicalRegistry<Class<? extends IGenericTaskModelUI>> models = new HierarchicalRegistry<Class<? extends IGenericTaskModelUI>>();

    private Map<Class<? extends IGenericTaskModelUI>, Preferences.Settings> modelSettingsMap =
            new HashMap<Class<? extends IGenericTaskModelUI>, Preferences.Settings>();

    private Map<Class<? extends IGenericTaskCapsule>,Class<? extends ICapsuleModelUI>> capsuleMapping = new HashMap();

    private Settings capsuleModelSettingsMap = new Settings(app.getColor(ApplicationCustomize.TASK_CAPSULE_BACKGROUND_COLOR),
                                                            app.getColor(ApplicationCustomize.TASK_CAPSULE_BORDER_COLOR));

    public void initialize(){
        setBusinessModelMap();
        setModelSettings();
        setCapsuleMapping();
    }

    private void setCapsuleMapping(){
        capsuleMapping.put(TaskCapsule.class,TaskCapsuleModelUI.class);
        capsuleMapping.put(ExplorationTaskCapsule.class,ExplorationTaskCapsuleModelUI.class);
    }

    private void setBusinessModelMap() {
        models.register(GroovyTask.class, GroovyTaskModelUI.class);
        models.register(JavaTask.class, TaskModelUI.class);
        models.register(ExplorationTask.class, ExplorationTaskModelUI.class);
    }

    public Set<Class> getBusinessClasses(){
        return models.getAllRegistred();
    }

    public Class<? extends ICapsuleModelUI> getCapsuleMapping(Class<? extends IGenericTaskCapsule> gtc){
        return capsuleMapping.get(gtc);
    }

    public Class<? extends IGenericTaskModelUI> getModelClass(Class c){
        return models.getClosestRegistred(c).iterator().next();
    }

    public void addModelSettings(Class<? extends IGenericTaskModelUI> modelClass,
                                  Preferences.Settings s){
        modelSettingsMap.put(modelClass, s);
    }

    private void setModelSettings(){
        ApplicationCustomize app = ApplicationCustomize.getInstance();

        modelSettingsMap.put(TaskModelUI.class, new Settings(app.getColor(ApplicationCustomize.TASK_BACKGROUND_COLOR),
                                                        app.getColor(ApplicationCustomize.TASK_BORDER_COLOR)));
        modelSettingsMap.put(ExplorationTaskModelUI.class, new Settings(app.getColor(ApplicationCustomize.EXPLORATION_TASK_BACKGROUND_COLOR),
                                                                   app.getColor(ApplicationCustomize.EXPLORATION_TASK_BORDER_COLOR)));
        modelSettingsMap.put(GroovyTaskModelUI.class, new Settings(app.getColor(ApplicationCustomize.GROOVY_TASK_BACKGROUND_COLOR),
                                                                   app.getColor(ApplicationCustomize.GROOVY_TASK_BORDER_COLOR),
                                                                   ApplicationCustomize.IMAGE_GROOVY));
    }

    public Settings getModelSettings(Class<? extends IGenericTaskModelUI> model){
        return modelSettingsMap.get(model);
    }

    public Settings getCapsuleModelSettings(){
        return capsuleModelSettingsMap;
    }

    public static Preferences getInstance() {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }

    public class Settings {
        private Color defaultBackgroundColor;
        private Color defaultBorderColor;
        private Image defaultBackgroundImage = null;

        public Settings(Color defaultBackgroundColor,
                        Color defaultBorderColor) {
            this.defaultBackgroundColor = defaultBackgroundColor;
            this.defaultBorderColor = defaultBorderColor;
        }

        public Settings(Color defaultBackgroundColor,
                        Color defaultBorderColor,
                        Image backgroundImaqe) {
            this(defaultBackgroundColor, defaultBorderColor);
            this.defaultBackgroundImage = backgroundImaqe;
        }

        public Image getDefaultBackgroundImage() {
            return defaultBackgroundImage;
        }

        public Color getDefaultBackgroundColor() {
            return defaultBackgroundColor;
        }

        public Color getDefaultBorderColor() {
            return defaultBorderColor;
        }


    }

}
