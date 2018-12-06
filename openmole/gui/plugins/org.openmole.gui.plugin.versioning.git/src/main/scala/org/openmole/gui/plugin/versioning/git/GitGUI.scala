/**
 * Created by Mathieu Leclaire on 23/04/18.
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
import org.openmole.gui.plugin.versioning.git.GitGUI.{ Cloning, CloningStatus, NotClonedYet }
import scaladget.bootstrapnative.bsn._
import org.openmole.core.services._

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitFactory")
class GitFactory extends VersioningPluginFactory {

  type APIType = GitAPI

  def api = (s: Services) ⇒ new GitApiImpl(s)

  def name: String = "GIT"

  def build(cloneIn: SafePath, onCloned: () ⇒ Unit = () ⇒ {}) = new GitGUI(cloneIn, onCloned)

  def versioningConfigFolderName = ".git"
}

object GitGUI {

  trait CloningStatus

  object NotClonedYet extends CloningStatus

  object Cloning extends CloningStatus

  object Cloned extends CloningStatus

  case class CloneError(messageError: MessageErrorData) extends CloningStatus

}

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitGUI")
class GitGUI(cloneIn: SafePath, onCloned: () ⇒ Unit = () ⇒ {}) extends VersioningGUIPlugin {

  def factory = new GitFactory

  import rx._

  val cloningStatus: Var[CloningStatus] = Var(NotClonedYet)
  val watchStack = Var(false)
  val inputStyle: ModifierSeq = Seq(width := 300)
  val repositoryUrlInput = inputTag("")(placeholder := "Repository HTTPS URL", inputStyle).render

  val repositoryUrlButton = button("clone", marginLeft := 5, btn_default, `type` := "submit")

  lazy val panel: TypedTag[HTMLElement] =
    div(
      form(paddingTop := 20, width := 500, display := "flex", marginLeft := 5)(
        repositoryUrlInput,
        Rx {
          cloningStatus() match {
            case Cloning ⇒ div(backgroundColor := client.DARK_GREY, width := 60, height := 33, marginLeft := 5, borderRadius := "4px")(Waiter.waiter)
            case _       ⇒ repositoryUrlButton
          }
        },
        Rx {
          cloningStatus() match {
            case GitGUI.CloneError(messageError: MessageErrorData) ⇒ label(btn_danger, "Cloning error", marginLeft := 5, onclick := { () ⇒ watchStack.update(!watchStack.now) })
            case _ ⇒ div
          }
        }
      )(
          onsubmit := {
            () ⇒
              cloningStatus.update(Cloning)
              cloneGIT.foreach { c ⇒
                println("Cloned : " + c)
                val status = c match {
                  case None ⇒
                    onCloned()
                    GitGUI.Cloned
                  case Some(me: MessageErrorData) ⇒ GitGUI.CloneError(me)
                }
                cloningStatus.update(status)
              }
              false
          }
        ),
      div(Rx {
        cloningStatus() match {
          case GitGUI.CloneError(messageError: MessageErrorData) ⇒ watchStack.expand(div(messageError.stackTrace))
          case _ ⇒ div.render
        }
      })
    )

  def cloneGIT = OMPost()[GitAPI].clone(repositoryUrlInput.value, cloneIn).call()
}
