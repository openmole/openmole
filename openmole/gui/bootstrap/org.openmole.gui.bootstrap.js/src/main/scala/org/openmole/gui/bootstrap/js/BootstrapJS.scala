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
  val webapp = new File(webui, "webapp")
  val jsCompiled = new File(webapp, "js")
  val jsSrc = new File(webui, "js/src")
  val authKeys = Workspace.file("persistent/keys")

  webui.mkdirs
  projects.mkdirs
  authKeys.mkdirs

  def init(optimized: Boolean = true) = {

    def update() = {
      webapp.recursiveDelete
      webapp.mkdirs
      jsCompiled.mkdirs

      new File(webapp, "css").mkdirs
      new File(webapp, "fonts").mkdirs
      new File(webapp, "img").mkdirs
      new File(webapp, "WEB-INF").mkdirs

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
      copyURL(thisBundle.findEntries("/", "*.png", true).asScala)
      copyURL(thisBundle.findEntries("/", "*.eot", true).asScala)

      //Generates the pluginMapping js file
      new java.io.File(jsCompiled, "pluginMapping.js").withWriter() { writer ⇒
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
      }

      JSPack(jsSrc, jsCompiled, optimized)
    }

    jsSrc.recursiveDelete
    jsSrc.mkdirs

    // Extract and copy all the .sjsir files from bundles to src
    for {
      b ← PluginManager.bundles
      entries ← Option(b.findEntries("/", "*.sjsir", true))
      entry ← entries.asScala
    } {
      val inputStream = entry.openStream
      try inputStream.copy(new java.io.File(jsSrc, entry.getFile.split("/").tail.mkString("-")))
      finally inputStream.close
    }

    //Generates js files if
    // - the sources changed or
    // - the optimized js does not exists in optimized mode or
    // - the not optimized js does not exists in not optimized mode
    jsSrc.updateIfChanged(_ ⇒ update())
    if (!new File(jsCompiled, JSPack.JS_FILE).exists) update()
  }

  private def copyWebJarResource(resourceName: String, version: String, file: String): Unit = copyWebJarResource(resourceName, version, FilePath("", file))

  private def copyWebJarResource(resourceName: String, version: String, filePaths: FilePath*): Unit =
    for (filePath ← filePaths)
      new File(jsCompiled, filePath.file).withOutputStream { fileStream ⇒
        getClass.getClassLoader.getResourceAsStream("/META-INF/resources/webjars/" + resourceName + "/" + version + "/" + filePath.path + filePath.file).copy(fileStream)
      }

  private def copyURL(url: Iterator[URL]) = url.foreach { u ⇒ u.openStream.copy(new File(webui, u.getFile)) }

  case class FilePath(path: String, file: String)

}