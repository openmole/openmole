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
import org.openmole.gui.server.jscompile.Webpack.ExtraModule

import scala.jdk.CollectionConverters._

object Plugins extends JavaLogger {

  def createJsPluginDirectory(jsPluginDirectory: File) = {
    jsPluginDirectory.mkdirs
    Plugins.gatherJSIRFiles(jsPluginDirectory)
    jsPluginDirectory
  }

  def gatherJSIRFiles(dest: File) = {
    import scala.jdk.CollectionConverters.*

    def bundles =
      PluginManager.bundles.filter { b ⇒
        !b.openMOLEScope.exists(_.toLowerCase == "gui-provided")
      }

    for
      b ← bundles
    do
      val jar = new java.util.jar.JarFile(b.file)
      try
        for
          entry <- jar.entries().asScala
          if entry.getName().endsWith(".sjsir")
        do
          val destFile = dest / entry.getName()
          destFile.getParentFile.mkdirs()
          val is = jar.getInputStream(entry)
          try is.copy(destFile, replace = true)
          finally is.close
      finally jar.close()

  /* This code does'nt copy the java.* jsir for some classloader reasons
    for {
      b ← bundles
      jsir ← Option(b.findEntries("/", "*.sjsir", true)).map(_.asScala).getOrElse(Seq.empty)
    } {
      val destFile = dest / jsir.getPath
      destFile.getParentFile.mkdirs()

      println(b.file)

      b.classLoader.getResourceAsStream(jsir.getPath) match {
        case null =>
        case s => s copy destFile
      }
    }*/
  }

  def openmoleFile(optimizedJS: Boolean)(implicit workspace: Workspace, newFile: TmpDirectory, fileService: FileService) = newFile.withTmpDir { jsPluginDirectory =>
    createJsPluginDirectory(jsPluginDirectory)

    val webui = workspace.persistentDir /> "webui"
    val jsPluginHash = workspace.persistentDir / "js-plugin-hash"
    val jsFile = webui / utils.openmoleFileName

    def update = {
      Log.logger.info("Building GUI plugins ...")

      DirUtils.deleteIfExists(webui)
      webui.mkdir

      JSPack.link(jsPluginDirectory, jsFile, optimizedJS)

      Log.logger.info("Webpacking ...")
      val webpackConfigTemplateLocation = GUIServer.webpackLocation / utils.webpackConfigTemplateName
      val webpackJsonPackage = GUIServer.webpackLocation / utils.webpackJsonPackage
      val webpackOutput = webui / utils.webpakedOpenmoleFileName

      val modeOpenMOLE = Plugins.expandDepsFile(GUIServer.fromWebAppLocation /> "js" / utils.openmoleGrammarName, webui / utils.openmoleGrammarMode)

      JSPack.webpack(jsFile, webpackJsonPackage, webpackConfigTemplateLocation, webpackOutput, Seq(ExtraModule(modeOpenMOLE, utils.aceModuleSource)))
    }

    (jsPluginDirectory / "optimized_mode").content = optimizedJS.toString

    if !jsFile.exists
    then
      update
      utils.updateIfChanged(jsPluginDirectory, Some(jsPluginHash)) { identity } // Make sure hash file is created
    else utils.updateIfChanged(jsPluginDirectory, Some(jsPluginHash)) { _ ⇒ update }

    webui / utils.webpakedOpenmoleFileName
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
          s""" "${rules._1.map { _.name }.mkString("|")}" """)
        .replace(
          "##OMClasses##",
          s""" "${rules._2.map { _.name }.mkString("|")}" """)

  }

//  def addPluginRoutes(route: OMRouter ⇒ Unit, services: Services) = {
//    Log.logger.info(s"Loading GUI plugins")
//    GUIPluginRegistry.routers.foreach { r ⇒ route(r(services)) }
//  }
}
