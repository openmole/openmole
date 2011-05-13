/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

import javax.swing.text.JTextComponent
import org.openide.text.ActiveEditorDrop
import org.openide.util.Lookup
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.IEnvironmentFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.EnvironmentUI
import org.openmole.ide.core.workflow.implementation.PrototypeUI
import org.openmole.ide.core.workflow.implementation.SamplingUI
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.JavaConversions._
import org.openmole.ide.core.exception.GUIUserBadDataError

//class PaletteElementFactory(val displayName: String,val factoryUI: IFactoryUI){
class PaletteElementFactory(val displayName: String, val thumbPath: String, val entityType: String, val factory: IFactoryUI){
  
  // def buildEntity = factoryUI.buildEntity(displayName,factoryUI.panel)
  // def buildNewEntity = factoryUI.buildEntity(factoryUI.panel)
  factory.panelUIData.name = displayName
  
  def buildEntity: IEntityUI = {
    entityType match {
      case Constants.TASK => new TaskUI(this)
      case Constants.PROTOTYPE => new PrototypeUI(this)
      case Constants.SAMPLING => new SamplingUI(this)
      case Constants.ENVIRONMENT => new EnvironmentUI(this)                         
      case _=> throw new GUIUserBadDataError("The entity " + entityType + " does not exist.")
    }
  }
  
 
//  def factoryInstance = factoryInstances.find{en:AnyRef => factoryUIClass.isAssignableFrom(en.getClass)}.get
//  
//  private def factoryInstances: Collection[IFactoryUI] = {
//    entityType match {
//      case Constants.TASK=> Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI])
//      case Constants.PROTOTYPE=> Lookup.getDefault.lookupAll(classOf[IPrototypeFactoryUI])
//      case Constants.SAMPLING=> Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI])
//      case Constants.ENVIRONMENT=> Lookup.getDefault.lookupAll(classOf[IEnvironmentFactoryUI])
//      case _=> throw new GUIUserBadDataError("The entity " + entityType + " does not exist.")
//    }
//  }
}
