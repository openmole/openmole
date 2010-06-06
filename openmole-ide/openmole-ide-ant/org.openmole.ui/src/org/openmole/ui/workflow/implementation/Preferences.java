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
import org.openmole.core.workflow.implementation.data.Prototype;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.misc.exception.UserBadDataError;
import org.openmole.ui.workflow.model.IObjectModelUI;
<<<<<<< HEAD
import org.openmole.commons.tools.service.HierarchicalRegistry;
import org.openmole.ui.exception.MoleExceptionManagement;
=======
import org.openmole.misc.tools.service.HierarchicalRegistry;
import org.openmole.ui.palette.Category.CategoryName;
>>>>>>> ide update

/**
 *
 * @author mathieu
 */
public class Preferences {

    private static Preferences instance = null;

    private CategoryName[] propertyTypes = {CategoryName.TASK, CategoryName.TASK_CAPSULE};
    private Map<CategoryName, HierarchicalRegistry<Class<? extends IObjectModelUI>>> models = new HashMap<CategoryName, HierarchicalRegistry<Class<? extends IObjectModelUI>>>();
    private Map<CategoryName, Map<Class, Properties>> properties = new HashMap<CategoryName, Map<Class, Properties>>();
    private Collection<Class> prototypeTypes = new ArrayList<Class>();


    private Map<String, PrototypeUI> prototypes = new HashMap<String, PrototypeUI>();

    public void register() {
        if (models.isEmpty()) {
            for (CategoryName c : propertyTypes) {
                PropertyManager.buildLookup(c);
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
        models.get(cat).register(coreClass, modelClass);
    }

    private void registerProperties(CategoryName cat,
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
            Class coreClass) throws UserBadDataError{
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

    private void setPrototypeTypes() {
        prototypeTypes.add(BigInteger.class);
        prototypeTypes.add(BigDecimal.class);
        prototypeTypes.add(File.class);
    }

    public Collection getPrototypeTypes() {
        if (prototypeTypes.isEmpty()) {
            setPrototypeTypes();
        }
        return prototypeTypes;
    }

     public void registerPrototype(PrototypeUI p) {
        prototypes.put(p.getName(), p);
    }

    public PrototypeUI getPrototype(String st) throws UserBadDataError {
        if (prototypes.containsKey(st)) {
            return prototypes.get(st);
        } else {
            throw new UserBadDataError("The prototype " + st + " doest not exist.");
        }
    }

    public Collection<PrototypeUI> getPrototypes(){
        prototypes.put("eieie", new PrototypeUI("eieie",BigInteger.class));
        return prototypes.values();
    }


    public Set<Class> getCoreTaskClasses() {
        register();
        return models.get(CategoryName.TASK).getAllRegistred();
    }

    public static Preferences getInstance() {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }
}
