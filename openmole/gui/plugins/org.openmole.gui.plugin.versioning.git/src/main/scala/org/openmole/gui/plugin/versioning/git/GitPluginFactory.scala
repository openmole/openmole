package org.openmole.gui.plugin.versioning.git

import org.openmole.core.services.Services
import org.openmole.gui.ext.data.{ SafePath, VersioningGUIPlugin, VersioningPluginFactory }

import scala.scalajs.js.annotation.JSExportTopLevel

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitPluginFactory")
class GitPluginFactory extends VersioningPluginFactory {

  type APIType = GitAPI

  def api = (s: Services) ⇒ new GitApiImpl(s)

  def name: String = "GIT"

  def versioningConfigFolderName = ".git"

  def clonePanel(cloneIn: SafePath, onCloned: () ⇒ Unit = () ⇒ {}) = new CloningPanel(cloneIn, onCloned)

  def diffPanel(localSafePath: SafePath, repositorySafePath: SafePath) = new DiffPanel(localSafePath, repositorySafePath)

}
