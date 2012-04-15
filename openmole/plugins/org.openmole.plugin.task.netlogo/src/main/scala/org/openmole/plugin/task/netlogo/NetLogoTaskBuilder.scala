/*
 * Copyright (C) 2012 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.netlogo

import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import org.openmole.plugin.task.external.ExternalTaskBuilder

abstract class NetLogoTaskBuilder extends ExternalTaskBuilder {
    
  var _netLogoInputs = List.empty[(IPrototype[_], String)]
  var _netLogoOutputs = List.empty[(String, IPrototype[_])]
  
  def netLogoInputs = new {
    def +=(p: IPrototype[_], n: String): NetLogoTaskBuilder.this.type = {
      _netLogoInputs ::= p -> n
      inputs += p
      NetLogoTaskBuilder.this
    }
    
    def +=(p: IPrototype[_]): NetLogoTaskBuilder.this.type = this.+=(p, p.name)
    
    def apply() = _netLogoInputs.reverse
  }
  
  def netLogoOutputs = new {
    def +=(n: String, p: IPrototype[_]): NetLogoTaskBuilder.this.type = {
      _netLogoOutputs ::= n -> p
      outputs += p
      NetLogoTaskBuilder.this
    }
    
    def +=(p: IPrototype[_]): NetLogoTaskBuilder.this.type = this.+=(p.name, p)
    
    def apply() = _netLogoOutputs.reverse
  }
  
}