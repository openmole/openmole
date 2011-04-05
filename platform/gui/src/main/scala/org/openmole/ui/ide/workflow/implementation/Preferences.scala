/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.ui.ide.palette.MoleConcepts
import org.openmole.misc.tools.service.HierarchicalRegistry
import java.util.Properties
import scala.collection.mutable.HashMap
import scala.collection.mutable.WeakHashMap

import org.openmole.ui.ide.workflow.model.IGenericTaskModelUI

object Preferences {
  val propertyTypes= List(MoleConcepts.TASK_INSTANCE, MoleConcepts.CAPSULE_INSTANCE, MoleConcepts.PROTOTYPE_INSTANCE,MoleConcepts.SAMPLING_INSTANCE) 
  var models= new HashMap[MoleConcepts.Concept,HierarchicalRegistry[Class[_]]]
  var properties= new HashMap[MoleConcepts.Concept,HashMap[Class[_],Properties]]
  var coreClasses= new WeakHashMap[Class[_],Class[_]]
  
  def clearModels()= models.clear
  
  def clearProperties()= properties.clear
  
  def register(): Unit= {
    if (models.isEmpty) propertyTypes.foreach(PropertyManager.readProperties(_))
  }
  
  def register(cat: MoleConcepts.Concept, coreClass: Class[_],prop: Properties): Unit= {
    // registerModel(cat,coreClass,ClassOf[prop.getProperty(PropertyManager.IMPL)])
    registerProperties(cat,coreClass,prop)
  }
  
  def registerModel(cat: MoleConcepts.Concept, coreClass: Class[_], modelClass: Class[_])= {
    if (models.isEmpty) propertyTypes.foreach(models.put(_,new HierarchicalRegistry[Class[_]]))
    coreClasses+= modelClass-> coreClass
    models(cat).register(coreClass, modelClass)
  }
  
  def registerProperties(cat: MoleConcepts.Concept, coreClass: Class[_], prop: Properties)= {
    if (properties.isEmpty) propertyTypes.foreach(properties+= _-> new HashMap[Class[_],Properties])
    properties(cat)+= coreClass-> prop
  }
  
  def properties(cat: MoleConcepts.Concept, coreClass: Class[_]): Properties = {
    register
    properties(cat)(coreClass)
  }
  
//  def taskModel(cat: MoleConcepts.Concept, coreClass: Class[_])= {
//    register
//    models(cat).closestRegistred(coreClass).iterator.next.asInstanceOf[IGenericTaskModelUI[_]]
//  }
  
  def model(cat: MoleConcepts.Concept, coreClass: Class[_])= {
    register
    models(cat).closestRegistred(coreClass).iterator.next 
  }
  
  def coreTaskClasses: Set[Class[_]]= {
    register
    models(MoleConcepts.TASK_INSTANCE).allRegistred
  }
  
  def prototypeTypeClasses: Set[Class[_]]= {
    register
    models(MoleConcepts.PROTOTYPE_INSTANCE).allRegistred
  }
  
  def samplingTypeClasses: Set[Class[_]]= {
  register
  models(MoleConcepts.SAMPLING_INSTANCE).allRegistred
  }
  
  
}
//    public Set<Class<?>> getCoreTaskClasses() {
//        register();
//        return models.get(Category.TASK_INSTANCE).allRegistred();
//    }

//    public Set<Class<?>> getPrototypeTypeClasses() {
//        register();
//        return models.get(Category.PROTOTYPE_INSTANCE).allRegistred();
//    }

//public Set<Class<?>> getSamplingTypeClasses(){
//  register();
//  return models.get(Category.SAMPLING_INSTANCE).allRegistred();
//}
//public Class<? extends IObjectModelUI> getModel(Category cat,
//            Class coreClass) throws UserBadDataError {
//        register();
//        try {
//            return models.get(cat).closestRegistred(coreClass).iterator().next();
//        } catch (ClassCastException ex) {
//            throw new UserBadDataError(ex);
//        } catch (NullPointerException ex) {
//            throw new UserBadDataError(ex);
//        }
//    }

//public Properties getProperties(Category cat,
//            Class coreClass) throws UserBadDataError {
//        register();
//        try {
//            return properties.get(cat).get(coreClass);
//        } catch (ClassCastException ex) {
//            throw new UserBadDataError(ex);
//        } catch (NullPointerException ex) {
//            throw new UserBadDataError(ex);
//        }
//    }
//
//private Category[] propertyTypes = {Category.TASK_INSTANCE, Category.CAPSULE_INSTANCE, Category.PROTOTYPE_INSTANCE,Category.SAMPLING_INSTANCE};
//    private Map<Category, HierarchicalRegistry<Class<? extends IObjectModelUI>>> models = new HashMap<Category, HierarchicalRegistry<Class<? extends IObjectModelUI>>>();
//    private Map<Category, Map<Class, Properties>> properties = new HashMap<Category, Map<Class, Properties>>();
//    private Map<Class<? extends IObjectModelUI>, Class> coreClasses = new WeakHashMap<Class<? extends IObjectModelUI>, Class>();
//public void registerProperties(Category cat,
//            Class coreClass,
//            Properties prop) {
//        if (properties.isEmpty()) {
//            for (Category c : propertyTypes) {
//                properties.put(c, new HashMap<Class, Properties>());
//            }
//        }
//        properties.get(cat).put(coreClass, prop);
//    }

//private void registerModel(Category cat,
//                           Class coreClass,
//                           Class<? extends IObjectModelUI> modelClass) {
//  if (models.isEmpty()) {
//    for (Category c : propertyTypes) {
//      models.put(c, new HierarchicalRegistry<Class<? extends IObjectModelUI>>());
//    }
//  }
//  coreClasses.put(modelClass, coreClass);
//  models.get(cat).register(coreClass, modelClass);
//}

//  public void register(Category cat,
//            Class coreClass,
//            Properties prop) throws ClassNotFoundException {
//        registerModel(cat,
//                coreClass,
//                (Class<? extends IObjectModelUI>) Class.forName(prop.getProperty(PropertyManager.IMPL)));
//        registerProperties(cat,
//                coreClass,
//                prop);
//    }

//    public void clearModels() {
//        models.clear();
//    }
//
//    public void clearProperties() {
//        properties.clear();
//    }

//    public void register() {
//        if (models.isEmpty()) {
//            for (Category c : propertyTypes) {
//                PropertyManager.readProperties(c);
//            }
//        }
//    }