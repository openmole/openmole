/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

import org.openide.util.Lookup
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.NativeFactories
import org.openmole.ide.core.commons.Constants
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object PaletteElementFactories {
  var paletteElements = Map(Constants.TASK -> new ListBuffer[PaletteElementFactory],
                            "TaskModel" -> new ListBuffer[PaletteElementFactory],
                            Constants.PROTOTYPE -> new ListBuffer[PaletteElementFactory],
                            "PrototypeModel" -> new ListBuffer[PaletteElementFactory],
                            Constants.SAMPLING -> new ListBuffer[PaletteElementFactory],
                            "SamplingModel" -> new ListBuffer[PaletteElementFactory])
  
  NativeFactories.prototypeFactories.foreach(p=>{paletteElements("PrototypeModel") += new PaletteElementFactory(p.coreClass.getSimpleName,p)})
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).foreach(t=>{paletteElements("TaskModel") += new PaletteElementFactory(t.coreClass.getSimpleName,t)})   
  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI]).foreach(s=>{paletteElements("SamplingModel") += new PaletteElementFactory(s.coreClass.getSimpleName,s)})    
  
  def addPrototypeElement(pef: PaletteElementFactory) = paletteElements(Constants.PROTOTYPE) += pef
  def addTaskElement(pef: PaletteElementFactory) = paletteElements(Constants.TASK) += pef
  
  def getFactoryUI(categoryName: String, name: String): IFactoryUI= {
    paletteElements(categoryName).groupBy(_.displayName).filterKeys(k => k.equals(name))(name).head.factoryUI
  }
}
//       new GenericCategory("Sampling models", new GenericChildren(JavaConversions.asScalaIterable(Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI])),new DataFlavor(classOf[SamplingUI], "Samplings"))),
      