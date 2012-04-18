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
import org.openmole.core.implementation.data._
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet
import scala.collection.mutable.ListBuffer

abstract class ExternalTaskBuilder extends TaskBuilder {
  
  private var _provided = new ListBuffer[(Either[File, IPrototype[File]], String, Boolean)]
  private var _produced = new ListBuffer[(String, IPrototype[File])]

  def provided = _provided.toList
  def produced = _produced.toList
  
    
  def addResource(file: File, name: String, link:  Boolean): ExternalTaskBuilder.this.type = {
    _provided += ((Left(file), name, link))
    this
  }
    
  def addResource(file: File, name: String): this.type = this.addResource(file, name, false)
  def addResource(file: File, link: Boolean): this.type = this.addResource(file, file.getName, link)
  def addResource(file: File): this.type = this.addResource(file, false)
    
    
  def addInput(p: IPrototype[File], name: String, link: Boolean ): this.type = {
    _provided += ((Right(p), name, link))
    this addInput p
    this
  }
    
  def addInput(p: IPrototype[File], name: String): ExternalTaskBuilder.this.type = this.addInput(p, name, false)
    
  def addOutput(name: String, p: IPrototype[File]): this.type = {
    _produced += ((name, p))
    this addOutput p
    this
  }
  
}
