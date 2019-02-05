package org.openmole.gui.plugin.versioning.git

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.client.OMPost
import scaladget.tools._
import autowire._
import org.openmole.gui.ext.tool.client
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.openmole.gui.ext.tool.client.Waiter
import org.openmole.gui.plugin.versioning.git.GitCloning.{ Cloning, CloningStatus, NotClonedYet }
import scaladget.bootstrapnative.bsn._
import org.openmole.core.services._

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.DiffPanel")
class DiffPanel(localSafePath: SafePath, repositorySafePah: SafePath) extends VersioningGUIPlugin {

  def factory = new GitPluginFactory

  import rx._

  lazy val panel: TypedTag[HTMLElement] = div()
}