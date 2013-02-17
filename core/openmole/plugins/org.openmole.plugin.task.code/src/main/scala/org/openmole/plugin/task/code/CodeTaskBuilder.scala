/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.task.code

import java.io.File
import org.openmole.plugin.task.external.ExternalTaskBuilder
import scala.collection.mutable.ListBuffer
import org.openmole.core.model.task.PluginSet

/**
 * Builder for any code task
 *
 * The code task builder is an external task builder, you may want to look
 * at the @see ExternalTaskBuilder for a complement of documentation.
 */
abstract class CodeTaskBuilder(implicit plugins: PluginSet) extends ExternalTaskBuilder { builder ⇒
  private var _imports = new ListBuffer[String]
  private var _libraries = new ListBuffer[File]

  def imports = _imports.toList
  def libraries = _libraries.toList

  /**
   * Add a namespace import and make it available to in the task
   *
   * For instance addImport("java.io.*") in a groovy task make the content of the
   * java.io package available in the groovy code.
   *
   * @param s a namespace
   */
  def addImport(s: String) = {
    _imports += s
    this
  }

  /**
   * Add a library and make it available to the task
   *
   * For instance addLib("/tmp/malib.jar") in a groovy task make the content of the
   * jar available to the task. This method support jars but has some limitation. The
   * best way to use your own bytecode (java, scala, groovy, jython) in OpenMOLE is
   * building an OpenMOLE plugin (see the section on openmole.org for details).
   *
   * @param l a jar file
   *
   */
  def addLib(l: File) = {
    _libraries += l
    this
  }

  trait Built extends super.Built {
    val imports = builder.imports
    val libraries = builder.libraries
  }

}