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

package org.openmole.plugin.task.external

import java.io.File
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet

abstract class ExternalTaskBuilder extends TaskBuilder {
  
  var _provided = List.empty[(Either[File, IPrototype[File]], String, Boolean)]
  var _produced = List.empty[(String, IPrototype[File])]

  def provided = new {
    
    def apply() = _provided.reverse
    
    def +=(file: File, name: String, link:  Boolean): ExternalTaskBuilder.this.type = {
       _provided ::= ((Left(file), name, link))
       ExternalTaskBuilder.this
    }
    
    def +=(file: File, name: String): ExternalTaskBuilder.this.type = this.+=(file, name, false)
    def +=(file: File, link: Boolean): ExternalTaskBuilder.this.type = this.+=(file, file.getName, link)
    def +=(file: File): ExternalTaskBuilder.this.type = this.+=(file, false)
    
    
    def +=(p: IPrototype[File], name: String, link: Boolean ): ExternalTaskBuilder.this.type = {
      _provided ::= ((Right(p), name, link))
      inputs += p
      ExternalTaskBuilder.this
    }
    
    def +=(p: IPrototype[File], name: String): ExternalTaskBuilder.this.type = this.+=(p, name, false)
  }
  
  def produced = new {
    
    def apply() = _produced.reverse
    
    def +=(name: String, p: IPrototype[File]): ExternalTaskBuilder.this.type = {
      _produced ::= ((name, p))
      outputs += p
      ExternalTaskBuilder.this
    }
    
  }
  
}
