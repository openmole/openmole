/*
 * Copyright (C) 2011 leclaire
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.palette

import org.openide.util.Lookup
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.IEnvironmentFactoryUI
import org.openmole.ide.core.properties.IFactoryUI
import org.openmole.ide.core.properties.IPrototypeFactoryUI
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.ITaskFactoryUI
import org.openmole.ide.core.workflow.implementation.EntityUI
import org.openmole.ide.core.workflow.implementation.EnvironmentUI
import org.openmole.ide.core.workflow.implementation.PrototypeUI
import org.openmole.ide.core.workflow.implementation.SamplingUI
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.workflow.model.IEntityUI
import scala.collection.JavaConversions._

class ModelElementFactory(val displayName: String, val entityType: String, val factory: IFactoryUI){
  
  private def buildEntity: IEntityUI = {
    entityType match {
      case Constants.TASK => new TaskUI(factory)
      case Constants.PROTOTYPE => new EntityUI(factory)
      case Constants.SAMPLING => new EntityUI(factory)
      case Constants.ENVIRONMENT => new EntityUI(factory)                         
      case _=> throw new GUIUserBadDataError("The entity " + entityType + " does not exist.")
    }
  }
  
  def buildPaletteElementFactory = new PaletteElementFactory(displayName,entityType,buildEntity)
}
  
//   def buildPaletteElementFactory = factoryInstances.find{en:AnyRef => factoryUIClass.isAssignableFrom(en.getClass)}.get
//  
//  private def factoryInstances: Collection[IFactoryUI] = {
//    entityType match {
//      case Constants.TASK_MODEL=> Lookup.getDefault.lookupAll(classOf[ITaskFactoryUI])
//      case Constants.PROTOTYPE_MODEL=> Lookup.getDefault.lookupAll(classOf[IPrototypeFactoryUI])
//      case Constants.SAMPLING_MODEL=> Lookup.getDefault.lookupAll(classOf[ISamplingFactoryUI])
//      case Constants.ENVIRONMENT_MODEL=> Lookup.getDefault.lookupAll(classOf[IEnvironmentFactoryUI])
//      case _=> throw new GUIUserBadDataError("The entity " + entityType + " does not exist.")
//    }
//  }
