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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.task.templatefilegenerator

import java.io.File
import org.openmole.core.model.data.IContext
import org.openmole.core.model.data.IPrototype

class TemplateFileGeneratorTask(name: String, template: File,outputPrototype: IPrototype[File]) extends AbstractTemplateFileGeneratorTask(name,outputPrototype){

  def this(name: String, templateName: String,outputPrototype: IPrototype[File]) = this(name,new File(templateName),outputPrototype)
  
  override def file(context: IContext) = template
}