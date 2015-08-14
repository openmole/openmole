/*
 * Copyright (C) 30/07/14 // mathieu.leclaire@openmole.org
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
package org.openmole.gui.bootstrap.js

import java.net.URL
import java.io.{ FileOutputStream, File }
import org.openmole.core.pluginmanager
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.tool.file._
import org.openmole.core.workspace.Workspace
import org.openmole.core.fileservice._
import org.openmole.gui.server.core.{ ServerFactories, GUIServer }
import org.openmole.gui.shared._
import scala.collection.JavaConverters._

object BootstrapJS {

  // Copy web resources and generate js file
  val webui = Workspace.file("webui")
  val projects = new File(webui, "projects")
  val jsSrc = new File(webui, "js/src")
  val webapp = new File(webui, "webapp")
  val jsCompiled = new File(webapp, "js")
  val authKeys = Workspace.file("persistent/keys")

  jsSrc.mkdirs
  jsCompiled.mkdirs
  webapp.mkdirs
  projects.mkdirs
  authKeys.mkdirs

  new File(webapp, "css").mkdirs
  new File(webapp, "fonts").mkdirs
  new File(webapp, "img").mkdirs
  new File(webapp, "WEB-INF").mkdirs

  def init(optimized: Boolean = true) = {

    //Copy all the fixed resources in the workspace if required
    val thisBundle = PluginManager.bundleForClass(classOf[GUIServer])

    //Add lib js files from webjars
    copyWebJarResource("d3", "3.5.5", "d3.min.js")
    copyWebJarResource("jquery", "2.1.3", "jquery.min.js")
    copyWebJarResource("bootstrap", "3.3.4", FilePath("dist/js/", "bootstrap.min.js"))
    copyWebJarResource("ace", "01.08.2014",
      FilePath("src-min/", "ace.js"),
      FilePath("src-min/", "theme-github.js"),
      FilePath("src-min/", "mode-scala.js"),
      FilePath("src-min/", "mode-sh.js"),
      FilePath("src-min/", "mode-text.js")
    )

    //All other resources
    copyURL(thisBundle.findEntries("/", "*.css", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.js", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.ttf", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.woff", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.woff2", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.svg", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.eot", true).asScala)

    // Extract and copy all the .sjsir files from bundles to src
    PluginManager.bundles.map { b ⇒
      b.findEntries("/", "*.sjsir", true)
    }.filterNot {
      _ == null
    }.flatMap {
      _.asScala
    }.map { u ⇒
      u.openStream.copy(new java.io.File(jsSrc, u.getFile.split("/").tail.mkString("-")))
    }

    //Generates the pluginMapping js file
    val writer = new java.io.FileWriter(new java.io.File(jsCompiled, "pluginMapping.js"))
    writer.write("function fillMap() {\n")
    ServerFactories.factoriesUI.foreach {
      case (k, v) ⇒
        writer.write("UIFactories().factoryMap[\"" + k + "\" ] = new " + v.getClass.getName + "();\n")
    }
    ServerFactories.authenticationFactoriesUI.foreach {
      case (k, v) ⇒
        writer.write("UIFactories().authenticationMap[\"" + k + "\" ] = new " + v.getClass.getName + "();\n")
    }
    writer.write("}")
    writer.close

    //Generates js files if
    // - the sources changed or
    // - the optimized js does not exists in optimized mode or
    // - the not optimized js does not exists in not optimized mode
    jsSrc.updateIfChanged(JSPack(_, jsCompiled, optimized))
    if (!new File(jsCompiled, JSPack.JS_FILE).exists)
      JSPack(jsSrc, jsCompiled, optimized)

  }

  def copyWebJarResource(resourceName: String, version: String, file: String): Unit = copyWebJarResource(resourceName, version, FilePath("", file))

  def copyWebJarResource(resourceName: String, version: String, filePaths: FilePath*): Unit = for (filePath ← filePaths) {
    val fileStream = new FileOutputStream(new File(webui, "webapp/js/" + filePath.file))
    getClass.getClassLoader.getResourceAsStream("/META-INF/resources/webjars/" + resourceName + "/" + version + "/" + filePath.path + filePath.file).copy(fileStream)
    fileStream.close
  }

  def copyURL(url: Iterator[URL]) = {
    url.foreach { u ⇒
      u.openStream.copy(new File(webui, u.getFile))
    }
  }

  case class FilePath(path: String, file: String)

}