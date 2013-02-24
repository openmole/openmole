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

package org.openmole.misc.osgi

import java.io.File
import java.net.{ URI, URLClassLoader }
import scala.collection.mutable.ArrayBuffer
import java.util.{ jar ⇒ juj }
import java.util.jar.{ Attributes, JarFile }
import org.eclipse.osgi.baseadaptor.loader._

object ClassPathBuilder {

  type AntLikeClassLoader = {
    def getClasspath: String
  }

  object AntLikeClassLoader {
    def unapply(ref: AnyRef): Option[AntLikeClassLoader] = {
      if (ref == null) return None
      try {
        val method = ref.getClass.getMethod("getClasspath")
        if (method.getReturnType == classOf[String])
          Some(ref.asInstanceOf[AntLikeClassLoader])
        else
          None
      } catch {
        case e: NoSuchMethodException ⇒ None
      }
    }
  }

  def getClassPathFrom(classLoader: ClassLoader): Seq[String] = classLoader match {
    case cl: URLClassLoader ⇒
      for (url ← cl.getURLs.toList; uri = new URI(url.toString); path = uri.getPath; if (path != null)) yield {
        // on windows the path can include %20
        // see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4466485
        // so lets use URI as a workaround
        new File(path).getCanonicalPath
        //val n = new File(uri.getPath).getCanonicalPath
        //if (n.contains(' ')) {"\"" + n + "\""} else {n}
      }

    case AntLikeClassLoader(acp) ⇒
      val cp = acp.getClasspath
      cp.split(File.pathSeparator)

    case bl: BaseClassLoader ⇒
      List(bl.getClasspathManager.getBaseData.getBundleFile.getBaseFile.getAbsolutePath)

    case _ ⇒
      throw new RuntimeException("Classloader not supported " + classLoader)

  }

}
