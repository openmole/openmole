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
package org.openmole.ui.ide.workflow.implementation;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.ui.ide.workflow.model.IObjectModelUI;
import org.openmole.commons.tools.service.HierarchicalRegistry;
import org.openmole.ui.ide.palette.Category.CategoryName;

/**
 *
 * @author mathieu
 */
public class Preferences {

    private static Preferences instance = null;
    private CategoryName[] propertyTypes = {CategoryName.TASK, CategoryName.CAPSULE, CategoryName.PROTOTYPE_INSTANCE};
    private Map<CategoryName, HierarchicalRegistry<Class<? extends IObjectModelUI>>> models = new HashMap<CategoryName, HierarchicalRegistry<Class<? extends IObjectModelUI>>>();
    private Map<CategoryName, Map<Class, Properties>> properties = new HashMap<CategoryName, Map<Class, Properties>>();
    private Map<Class<? extends IObjectModelUI>, Class> coreClasses = new WeakHashMap<Class<? extends IObjectModelUI>, Class>();

    public void clearModels() {
        models.clear();
    }

    public void clearProperties() {
        properties.clear();
    }

    public void register() {
        if (models.isEmpty()) {
            for (CategoryName c : propertyTypes) {
                PropertyManager.readProperties(c);
            }
        }
    }

    public void register(CategoryName cat,
            Class coreClass,
            Properties prop) throws ClassNotFoundException {
        registerModel(cat,
                coreClass,
                (Class<? extends IObjectModelUI>) Class.forName(prop.getProperty(PropertyManager.IMPL)));
        registerProperties(cat,
                coreClass,
                prop);
    }

    private void registerModel(CategoryName cat,
            Class coreClass,
            Class<? extends IObjectModelUI> modelClass) {
        if (models.isEmpty()) {
            for (CategoryName c : propertyTypes) {
                models.put(c, new HierarchicalRegistry<Class<? extends IObjectModelUI>>());
            }
        }
        coreClasses.put(modelClass, coreClass);
        models.get(cat).register(coreClass, modelClass);
    }

    public void registerProperties(CategoryName cat,
            Class coreClass,
            Properties prop) {
        if (properties.isEmpty()) {
            for (CategoryName c : propertyTypes) {
                properties.put(c, new HashMap<Class, Properties>());
            }
        }
        properties.get(cat).put(coreClass, prop);
    }

    public Properties getProperties(CategoryName cat,
            Class coreClass) throws UserBadDataError {
        register();
        try {
            return properties.get(cat).get(coreClass);
        } catch (ClassCastException ex) {
            throw new UserBadDataError(ex);
        } catch (NullPointerException ex) {
            throw new UserBadDataError(ex);
        }
    }

    public Class<? extends IObjectModelUI> getModel(CategoryName cat,
            Class coreClass) throws UserBadDataError {
        register();
        try {
            return models.get(cat).getClosestRegistred(coreClass).iterator().next();
        } catch (ClassCastException ex) {
            throw new UserBadDataError(ex);
        } catch (NullPointerException ex) {
            throw new UserBadDataError(ex);
        }
    }

    public Class getCoreClass(Class<? extends IObjectModelUI> cl) {
        return coreClasses.get(cl);
    }

    public Set<Class> getCoreTaskClasses() {
        register();
        return models.get(CategoryName.TASK).getAllRegistred();
    }

    public Set<Class> getPrototypeTypeClasses() {
        register();
        return models.get(CategoryName.PROTOTYPE_INSTANCE).getAllRegistred();
    }
    
    public static Preferences getInstance() {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }
}
