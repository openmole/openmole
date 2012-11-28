/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.misc.tools.util

import java.io.File
import groovy.lang.GroovyShell
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.exception.UserBadDataError

object ClassLoader {

  def toClass(s: String) = s match {
    case "Byte" ⇒ classOf[Byte]
    case "Short" ⇒ classOf[Short]
    case "Int" ⇒ classOf[Int]
    case "int" ⇒ classOf[Int]
    case "Long" ⇒ classOf[Long]
    case "long" ⇒ classOf[Long]
    case "Float" ⇒ classOf[Float]
    case "Double" ⇒ classOf[Double]
    case "double" ⇒ classOf[Double]
    case "Char" ⇒ classOf[Char]
    case "Boolean" ⇒ classOf[Boolean]
    case "String" ⇒ classOf[String]
    case "File" ⇒ classOf[java.io.File]
    case "BigInteger" ⇒ classOf[java.math.BigInteger]
    case "BigDecimal" ⇒ classOf[java.math.BigDecimal]
    case _ ⇒ try {
      classOf[GroovyShell].getClassLoader.loadClass(s)
    } catch {
      case e: ClassNotFoundException ⇒ throw new UserBadDataError("The class " + s + " has not been found")
    }
  }

  def toManifest(s: String) = nanifest(toClass(s))

  def loadExtraPlugins = {
    Workspace.pluginDirLocation.list.map {
      f ⇒ new File(Workspace.pluginDirLocation + "/" + f)
    } foreach {
      PluginManager.load
    }
  }
}