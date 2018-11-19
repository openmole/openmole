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
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.openmole.gui.ext.tool.client
import org.scalajs.dom.raw.HTMLElement

import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.openmole.gui.ext.tool.client.Waiter

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitFactory")
class GitFactory extends VersioningPluginFactory {

  def name: String = "GIT"

  def build(cloneIn: SafePath, onCloned: () ⇒ Unit = () ⇒ {}) = new GitGUI(cloneIn, onCloned)
}

@JSExportTopLevel("org.openmole.gui.plugin.versioning.git.GitGUI")
class GitGUI(cloneIn: SafePath, onCloned: () ⇒ Unit = () ⇒ {}) extends VersioningGUIPlugin {

  def factory = new GitFactory

  import rx._

  val cloning = Var(false)
  val inputStyle: ModifierSeq = Seq(width := 300)
  val repositoryUrlInput = inputTag("")(placeholder := "Repository URL", inputStyle).render

  val repositoryUrlButton = button("clone", btn_default, `type` := "submit")

  lazy val panel: TypedTag[HTMLElement] = div(
    hForm(Seq(paddingTop := 20, width := 500).toMS)(
      repositoryUrlInput,
      div(
        Rx {
          if (cloning()) div(backgroundColor := client.DARK_GREY, width := 60, height := 33, borderRadius := "4px")(Waiter.waiter)
          else repositoryUrlButton
        }).render
    )(
        onsubmit := {
          () ⇒
            cloning.update(true)
            cloneGIT.foreach { c ⇒
              cloning.update(false)
              onCloned()
            }
            false
        }
      ).render
  )

  def cloneGIT = OMPost()[GitAPI].cloneGIT(repositoryUrlInput.value, cloneIn).call()
}
