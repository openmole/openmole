/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

import org.openide.util.Lookup
import org.openmole.ide.core.properties.IEnvironmentFactoryUI
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.commons.Constants
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object ElementFactories {
  
  lazy val paletteElements = Map(Constants.TASK -> new ListBuffer[PaletteElementFactory],
                                 Constants.PROTOTYPE -> new ListBuffer[PaletteElementFactory],
                                 Constants.SAMPLING -> new ListBuffer[PaletteElementFactory],
                                 Constants.ENVIRONMENT -> new ListBuffer[PaletteElementFactory])
  
  lazy val modelElements = Map(Constants.TASK_MODEL -> updateLookup(classOf[ITaskFactoryUI],Constants.TASK_MODEL),
                               Constants.PROTOTYPE_MODEL -> updateLookup(classOf[IPrototypeFactoryUI],Constants.PROTOTYPE_MODEL),
                               Constants.SAMPLING_MODEL -> updateLookup(classOf[ISamplingFactoryUI],Constants.SAMPLING_MODEL),
                               Constants.ENVIRONMENT_MODEL -> updateLookup(classOf[IEnvironmentFactoryUI],Constants.ENVIRONMENT_MODEL))
  
  def updateLookup(factoryClass: Class[_<:IFactoryUI], entityType: String) = {
    val li = new ListBuffer[ModelElementFactory]
    Lookup.getDefault.lookupAll(factoryClass).foreach(p=>{println("loo loo : "+ p.coreClass.getSimpleName);li += new ModelElementFactory(p.coreClass.getSimpleName,p.imagePath,entityType,p.getClass)})
    li
  }
  
  def addElement(pef: PaletteElementFactory) =  {
    paletteElements(pef.entityType) += pef
  }
  
  def getPaletteElementFactory(categoryName: String, name: String): PaletteElementFactory= {
    paletteElements(categoryName).groupBy(_.displayName).filterKeys(k => k.equals(name))(name).head
  }
} 