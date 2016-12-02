/**
 * Created by Romain Reuillon on 30/11/16.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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
 *
 */
package org.openmole.gui.server.core

import org.openmole.core.pluginmanager._
import org.openmole.tool.file._
import org.openmole.tool.stream._
import collection.JavaConverters._

object Plugins {

  def gatherJSIRFiles(dest: File) = {
    def bundles =
      PluginManager.bundles.filter { b ⇒
        !b.openMOLEScope.exists(_.toLowerCase == "gui-provided")
      }

    for {
      b ← bundles
      jsir ← Option(b.findEntries("/", "*.sjsir", true)).map(_.asScala).getOrElse(Seq.empty)
    } {
      val destFile = dest / jsir.getPath
      destFile.getParentFile.mkdirs()
      b.classLoader.getResourceAsStream(jsir.getPath) copy destFile
    }
  }

  //  def init(optimized: Boolean = true) = {
  //    jsSrc.recursiveDelete
  //    jsSrc.mkdirs
  //
  //    def update() = {
  //      webapp.recursiveDelete
  //      webapp.mkdirs
  //      jsCompiled.mkdirs
  //
  //      new File(webapp, "css").mkdirs
  //      new File(webapp, "fonts").mkdirs
  //      new File(webapp, "img").mkdirs
  //      new File(webapp, "WEB-INF").mkdirs
  //
  //      //Copy all the fixed resources in the workspace if required
  //      val thisBundle = PluginManager.bundleForClass(classOf[GUIServer])
  //
  //      //Add lib js files from webjars
  //      /* copyWebJarResource("d3", "3.5.5", "d3.min.js")
  //       copyWebJarResource("jquery", "2.1.3", "jquery.min.js")
  //       copyWebJarResource("bootstrap", "3.3.4", FilePath("dist/js/", "bootstrap.min.js"))
  //       copyWebJarResource("ace", "01.08.2014",
  //         FilePath("src-min/", "ace.js"),
  //         FilePath("src-min/", "theme-github.js"),
  //         FilePath("src-min/", "mode-scala.js"),
  //         FilePath("src-min/", "mode-sh.js"),
  //         FilePath("src-min/", "mode-text.js"))*/
  //
  //      //All other resources
  //      /* copyURL(thisBundle.findEntries("/", "*.css", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.js", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.ttf", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.woff", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.woff2", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.svg", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.png", true).asScala)
  //      copyURL(thisBundle.findEntries("/", "*.eot", true).asScala)*/
  //
  //      //Generates the pluginMapping js file
  //      new java.io.File(jsCompiled, "pluginMapping.js").withWriter() { writer ⇒
  //        writer.write("function fillMap() {\n")
  //        /*   ServerFactories.factoriesUI.foreach {
  //          case (k, v) ⇒
  //            writer.write("UIFactories().factoryMap[\"" + k + "\" ] = new " + v.getClass.getName + "();\n")
  //        }
  //        ServerFactories.authenticationFactoriesUI.foreach {
  //          case (k, v) ⇒
  //            writer.write("UIFactories().authenticationMap[\"" + k + "\" ] = new " + v.getClass.getName + "();\n")
  //        }*/
  //        writer.write("}")
  //      }
  //      JSPack.link(Seq(copyJar("scalajs-library"), jsSrc), new java.io.File(jsCompiled, JS_FILE))
  //    }
  //
  //    // Extract and copy all the .sjsir files from bundles to src
  //    for {
  //      b ← PluginManager.bundles
  //      entries ← Option(b.findEntries("/", "*.sjsir", true))
  //      entry ← entries.asScala
  //    } entry.openStream.copy(new java.io.File(jsSrc, entry.getFile.split("/").tail.mkString("-")))
  //
  //    //Generates js files if
  //    // - the sources changed or
  //    // - the optimized js does not exists in optimized mode or
  //    // - the not optimized js does not exists in not optimized mode
  //    jsSrc.updateIfChanged(_ ⇒ update())
  //    if (!new File(jsCompiled, JS_FILE).exists) update()
  //  }

}
