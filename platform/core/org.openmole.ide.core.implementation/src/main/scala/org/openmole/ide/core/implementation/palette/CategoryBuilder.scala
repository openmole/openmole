/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.palette

import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.palette.ICategory
import org.openmole.ide.core.implementation.palette.PaletteSupport._
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openide.nodes.Node
import org.openide.nodes.Children
import org.openmole.ide.core.model.commons.MoleSceneType._
import scala.collection.JavaConversions._

class CategoryBuilder(val sceneType: MoleSceneType) extends Children.Keys[ICategory]{
  
  override protected def createNodes(key: ICategory) = Array[Node](new CategoryNode(key.asInstanceOf[ICategory]))
  
  override def addNotify = {
    super.addNotify
    
    setKeys(sceneType match {
        case BUILD=>List(
            new GenericCategory(TASK,"Tasks" ,new GenericChildren(Proxys.task,TASK_DATA_FLAVOR)),
            new GenericCategory(PROTOTYPE,"Prototypes" ,new GenericChildren(Proxys.prototype,PROTOTYPE_DATA_FLAVOR)),
            new GenericCategory(SAMPLING,"Samplings" ,new GenericChildren(Proxys.sampling,SAMPLING_DATA_FLAVOR))).toIterable
        case EXECUTION => List (
            new GenericCategory(ENVIRONMENT,"Environments" ,new GenericChildren(Proxys.environment,ENVIRONMENT_DATA_FLAVOR))).toIterable
      })
  }
            
}