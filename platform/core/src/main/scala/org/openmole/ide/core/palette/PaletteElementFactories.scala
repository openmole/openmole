/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

import org.openide.util.Lookup
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.properties.NativeFactories
import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object PaletteElementFactories {
  var prototypeModelPaletteElementFactories = new ListBuffer[PaletteElementFactory]
  var prototypePaletteElementFactories = new ListBuffer[PaletteElementFactory]
  val taskModelPaletteElementFactories = new ListBuffer[PaletteElementFactory]
  var taskPaletteElementFactories = new ListBuffer[PaletteElementFactory]
  val samplingModelPaletteElementFactories = new ListBuffer[PaletteElementFactory]
  
  NativeFactories.prototypeFactories.foreach(p=>{prototypeModelPaletteElementFactories += new PaletteElementFactory(p.coreClass.getSimpleName,p)})
  Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI]).foreach(t=>{taskModelPaletteElementFactories += new PaletteElementFactory(t.coreClass.getSimpleName,t)})   
  Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI]).foreach(s=>{samplingModelPaletteElementFactories += new PaletteElementFactory(s.coreClass.getSimpleName,s)})    
  
  def addPrototypeElement(pef: PaletteElementFactory) = prototypePaletteElementFactories += pef
  def addTaskElement(pef: PaletteElementFactory) = taskPaletteElementFactories += pef
}
 //       new GenericCategory("Sampling models", new GenericChildren(JavaConversions.asScalaIterable(Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI])),new DataFlavor(classOf[SamplingUI], "Samplings"))),
      