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
package org.openmole.ui.ide.dialog

import org.openmole.ui.ide.implementation.Preferences
import org.openmole.ui.ide.workflow.implementation.SamplingUI
import org.openmole.ui.ide.workflow.implementation.SamplingsUI

class SamplingManager extends IManager{

  override def entityInstance(name: String,t: Class[_])= {
    new SamplingUI(name, t)
  }
  
  override def container= SamplingsUI
  
  override def classTypes= Preferences.samplingTypeClasses
}

//public class SamplingManager implements IManager{
//
//    @Override
//    public IEntityUI getEntityInstance(String name, Class type) {
//        return new SamplingUI(name, type);
//    }
//
//    @Override
//    public IContainerUI getContainer() {
//        return SamplingsUI.getInstance();
//    }
//
//    @Override
//    public Set<Class<?>> getClassTypes() {
//        return Preferences.getInstance().getSamplingTypeClasses();
//    }
//
//}