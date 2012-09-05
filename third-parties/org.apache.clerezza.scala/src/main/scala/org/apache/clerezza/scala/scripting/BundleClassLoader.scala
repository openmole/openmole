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

package org.apache.clerezza.scala.scripting

import org.osgi.framework.Bundle

/**
 * A helper class to determine if the class loader provides access to an OSGi Bundle instance
 */
object BundleClassLoader {

  type BundleClassLoader = {
    def getBundle: Bundle
  }

  def unapply(ref: AnyRef): Option[BundleClassLoader] = {
    if (ref == null) return None
    try {
      val method = ref.getClass.getMethod("getBundle")
      if (method.getReturnType == classOf[Bundle])
        Some(ref.asInstanceOf[BundleClassLoader])
      else
        None
    } catch {
      case e: NoSuchMethodException ⇒ None
    }
  }
}