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

package org.openmole.ide.core.dialog

import org.openmole.ide.core.workflow.implementation.Preferences
import org.openmole.ide.core.workflow.implementation.PrototypeUI
import org.openmole.ide.core.workflow.implementation.PrototypesUI

class PrototypeManager extends IManager{

  override def entityInstance(name: String,t: Class[_])= new PrototypeUI(name, t)
  
  override def container= PrototypesUI
  
  override def classTypes= Preferences.prototypeTypeClasses
}
//import org.openmole.ide.core.implementation.Preferences;
//import org.openmole.ide.core.workflow.implementation.IContainerUI;
//import org.openmole.ide.core.workflow.implementation.IEntityUI;
//import org.openmole.ide.core.workflow.implementation.PrototypeUI;
//import org.openmole.ide.core.workflow.implementation.PrototypesUI;
//import scala.collection.immutable.Set;
//
///**
// *
// * @author Mathieu Leclaire <mathieu.leclaire@openmole.org>
// */
//public class PrototypeManager implements IManager {
//
//    @Override
//    public IEntityUI getEntityInstance(String name, Class type) {
//        return new PrototypeUI(name, type);
//    }
//
//    @Override
//    public IContainerUI getContainer() {
//        return PrototypesUI.getInstance();
//    }
//
//    @Override
//    public Set<Class<?>> getClassTypes() {
//        return Preferences.getPrototypeTypeClasses();
//    }
//}
