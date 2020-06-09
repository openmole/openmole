package org.openmole.gui.server.core

import org.openmole.core.fileservice._
import org.openmole.core.highlight.HighLight
import org.openmole.core.pluginmanager._
import org.openmole.tool.file._
import org.openmole.tool.stream._
import org.openmole.core.pluginmanager._
import org.openmole.core.pluginregistry._
import org.openmole.core.workspace._
import org.openmole.gui.server.jscompile.JSPack
import org.openmole.tool.logger.JavaLogger
import org.openmole.core.services.Services
import org.openmole.gui.ext.server._

import collection.JavaConverters._

object Plugins extends JavaLogger {

  def updateJsPluginDirectory(jsPluginDirectory: File) = {
    jsPluginDirectory.recursiveDelete
    jsPluginDirectory.mkdirs
    Plugins.gatherJSIRFiles(jsPluginDirectory)
    jsPluginDirectory
  }

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

  def openmoleFile(optimizedJS: Boolean)(implicit workspace: Workspace, newFile: TmpDirectory, fileService: FileService) = {
    val jsPluginDirectory = utils.webUIDirectory / "jsplugin"
    updateJsPluginDirectory(jsPluginDirectory)

    val jsFile = workspace.persistentDir /> "webui" / utils.openmoleFileName

    def update = {
      Log.logger.info("Building GUI plugins ...")
      jsFile.delete
      JSPack.link(jsPluginDirectory, jsFile, optimizedJS)
    }

    (jsPluginDirectory / "optimized_mode").content = optimizedJS.toString

    if (!jsFile.exists) update
    else utils.updateIfChanged(jsPluginDirectory) { _ ⇒ update }
    jsFile
  }

  def expandDepsFile(template: File, to: File) = {
    val rules = PluginRegistry.highLights.partition { kw ⇒
      kw match {
        case _@ (HighLight.TaskHighLight(_) | HighLight.SourceHighLight(_) | HighLight.EnvironmentHighLight(_) | HighLight.HookHighLight(_) | HighLight.SamplingHighLight(_) | HighLight.DomainHighLight(_) | HighLight.PatternHighLight(_)) ⇒ false
        case _ ⇒ true
      }
    }

    to.content =
      s"""${template.content}""" // ${AceOpenMOLEMode.content}
        .replace(
          "##OMKeywords##",
          s""" "${
            rules._1.map {
              _.name
            }.mkString("|")
          }" """)
        .replace(
          "##OMClasses##",
          s""" "${
            rules._2.map {
              _.name
            }.mkString("|")
          }" """)

  }

  def addPluginRoutes(route: OMRouter ⇒ Unit, services: Services) = {
    Log.logger.info(s"Loading GUI plugins")
    GUIPlugin.routers.foreach { r ⇒ route(r(services)) }
  }
}
