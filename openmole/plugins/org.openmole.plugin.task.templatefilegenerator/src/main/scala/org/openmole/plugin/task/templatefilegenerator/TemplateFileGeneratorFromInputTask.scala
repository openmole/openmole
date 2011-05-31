/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.plugin.task.templatefilegenerator

import java.io.File
import org.openmole.core.implementation.data.Data
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IData
import org.openmole.core.model.data.IPrototype

class TemplateFileGeneratorFromInputTask(name: String, templateFile: IData[File],outputPrototype: IPrototype[File]) extends AbstractTemplateFileGeneratorTask(name,outputPrototype){

  def this(name: String,tF: IPrototype[File],outputPrototype: IPrototype[File]) = this(name,new Data[File](tF),outputPrototype)
  addInput(templateFile)
  
  override def file(context: IContext) = {
    context.value(templateFile.prototype).get
  }
}