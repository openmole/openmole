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

import java.io.File
import java.net.URL

import org.openmole.gui.ext.dataui.DataUIs
import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.ext.panelui.PanelUI
import org.openmole.gui.misc.js.ClassKeyAggregator
import org.openmole.gui.server.core.GUIServer
import org.openmole.gui.client.core.GraphCreator
import org.openmole.gui.client.service.UIFactories
import org.openmole.gui.server.factory.ServerFactories
import org.openmole.gui.shared._
import autowire.Client
import org.openmole.misc.osgi.Activator
import org.openmole.misc.pluginmanager
import org.openmole.misc.pluginmanager.PluginManager
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.FileUtil
import org.openmole.misc.tools.io.FileUtil._
import org.osgi.framework.Bundle
import scala.collection.JavaConverters._
import org.openmole.misc.fileservice._

object BootstrapJS {

  // Copy web resources and generate js file
  val webui = Workspace.file("webui")
  val jsSrc = new File(webui, "js/src")
  val webapp = new File(webui, "webapp")
  val jsCompiled = new File(webapp, "js")
  jsSrc.mkdirs
  jsCompiled.mkdirs
  webapp.mkdirs

  new File(webapp, "css").mkdirs
  new File(webapp, "fonts").mkdirs
  new File(webapp, "WEB-INF").mkdirs

  def init(pluginBundles: List[Bundle], optimized: Boolean = true) = {

    //Copy all the fixed resources in the workspace if required
    val thisBundle = PluginManager.bundleForClass(classOf[GUIServer])
    copyURL(thisBundle.findEntries("/", "*.js", true).asScala)
    copyURL(thisBundle.findEntries("/", "*.css", true).asScala)
    copyURL(thisBundle.findEntries("/", "web.xml", true).asScala)

    // Extract and copy all the .sjsir files from bundles to src
    val bundles = pluginBundles ++ Seq(
      PluginManager.bundleForClass(classOf[GraphCreator]),
      PluginManager.bundleForClass(classOf[DataUIs]),
      PluginManager.bundleForClass(classOf[ClassKeyAggregator]),
      PluginManager.bundleForClass(classOf[FactoryUI]),
      PluginManager.bundleForClass(classOf[PanelUI]),
      PluginManager.bundleForClass(classOf[Api]),
      PluginManager.bundleForClass(classOf[UIFactories]),
      PluginManager.bundleForClass(classOf[autowire.Client[String, upickle.Reader, upickle.Writer]]),
      PluginManager.bundleForClass(classOf[org.scalajs.dom.HTMLHtmlElement]),
      PluginManager.bundleForClass(classOf[scalatags.DataConverters]),
      PluginManager.bundleForClass(classOf[upickle.Reader[_]]),
      PluginManager.bundleForClass(classOf[rx.Rx[_]])
    )

    bundles.map { b ⇒
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
        println("process " + k)
        writer.write("PluginMap().factoryMap[\"" + k + "\" ] = new " + v.getClass.getCanonicalName + "();\n")
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

  def copyURL(url: Iterator[URL]) = {
    url.foreach { u ⇒
      u.openStream.copy(new File(webui, u.getFile))
    }
  }

}