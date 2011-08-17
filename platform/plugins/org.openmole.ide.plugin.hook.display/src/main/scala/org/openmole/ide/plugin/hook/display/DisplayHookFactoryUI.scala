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

package org.openmole.ide.plugin.hook.display

import java.io.PrintStream
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.mutable.HashMap

class DisplayHookFactoryUI extends IHookFactoryUI {
  override def buildPanelUI(execution: IMoleExecution, 
                            prototypes: HashMap[IPrototypeDataProxyUI,IPrototype[_]], 
                            capsuleUI: ICapsuleUI, 
                            capsule: ICapsule,
                            printStream: PrintStream) = new DisplayHookPanelUI(execution,prototypes,capsuleUI,capsule,printStream)
}            
