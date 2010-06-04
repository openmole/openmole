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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.openmole.ui.workflow.model.IObjectModelUI;
import org.openmole.commons.tools.service.HierarchicalRegistry;
import org.openmole.ui.exception.MoleExceptionManagement;

/**
 *
 * @author mathieu
 */
public class Preferences {

    private static Preferences instance = null;

    private String[] propertyTypes = {PropertyManager.TASK, PropertyManager.TASK_CAPSULE};
    private Map<String, HierarchicalRegistry<Class<? extends IObjectModelUI>>> models = new HashMap<String, HierarchicalRegistry<Class<? extends IObjectModelUI>>>();
    private Map<String, Map<Class, Properties>> properties = new HashMap<String, Map<Class, Properties>>();
    private Collection<Class> prototypes = new ArrayList<Class>();

    public void register() {
        if (models.isEmpty()) {
            for (String t : propertyTypes) {
                PropertyManager.buildLookup(t);
            }
        }
    }

    public void register(String type,
            Class coreClass,
            Properties prop) throws ClassNotFoundException {
        registerModel(type,
                coreClass,
                (Class<? extends IObjectModelUI>) Class.forName(prop.getProperty(PropertyManager.IMPL)));
        registerProperties(type,
                coreClass,
                prop);
    }

    private void registerModel(String type,
            Class coreClass,
            Class<? extends IObjectModelUI> modelClass) {
        if (models.isEmpty()) {
            for (String t : propertyTypes) {
                models.put(t, new HierarchicalRegistry<Class<? extends IObjectModelUI>>());
            }
        }
        models.get(type).register(coreClass, modelClass);
    }

    private void registerProperties(String type,
            Class coreClass,
            Properties prop) {
        if (properties.isEmpty()) {
            for (String t : propertyTypes) {
                properties.put(t, new HashMap<Class, Properties>());
            }
        }
        properties.get(type).put(coreClass, prop);
    }

    public Properties getProperties(String type,
            Class coreClass) {
        register();
        try {
            return properties.get(type).get(coreClass);
        } catch (ClassCastException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (NullPointerException ex) {
            MoleExceptionManagement.showException(ex);
        }
        return null;
    }

    public Class<? extends IObjectModelUI> getModel(String type,
            Class coreClass) {
        register();
        try {
            return models.get(type).getClosestRegistred(coreClass).iterator().next();
        } catch (ClassCastException ex) {
            MoleExceptionManagement.showException(ex);
        } catch (NullPointerException ex) {
            MoleExceptionManagement.showException(ex);
        }
        return null;
    }

    private void setPrototypes() {
        prototypes.add(BigInteger.class);
        prototypes.add(BigDecimal.class);
        prototypes.add(File.class);
    }

    public Collection getPrototypes() {
        if (prototypes.isEmpty()) {
            setPrototypes();
        }
        return prototypes;
    }

    public Set<Class> getCoreTaskClasses() {
        register();
        return models.get(PropertyManager.TASK).getAllRegistred();
    }

    public static Preferences getInstance() {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }
}
