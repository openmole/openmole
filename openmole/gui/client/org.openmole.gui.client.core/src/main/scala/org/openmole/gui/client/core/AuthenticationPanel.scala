package org.openmole.gui.client.core

/*
 * Copyright (C) 17/05/15 // mathieu.leclaire@openmole.org
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

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.tags
import org.openmole.gui.ext.tool.client._
import org.openmole.gui.ext.tool.client.JsRxTags._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.ext.data._
import sheet._
import rx._
import bs._

class AuthenticationPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  case class TestedAuthentication(auth: AuthenticationPlugin, tests: Seq[Test])

  lazy val setting: Var[Option[AuthenticationPlugin]] = Var(None)
  private lazy val auths: Var[Seq[TestedAuthentication]] = Var(Seq())
  lazy val initialCheck = Var(false)

  def getAuthentications = {
    Plugins.authenticationFactories.now.map { factory ⇒
      val data = factory.getData
      data.map { d ⇒
        auths() = d.map {
          factory.build
        }.map { auth ⇒ TestedAuthentication(auth, Seq(PendingTest)) }
        testAuthentications
      }
    }
  }

  val authenticationSelector = {
    Plugins.authenticationFactories.map {
      _.options(0, btn_primary, (a: AuthenticationPluginFactory) ⇒ a.name, onclickExtra = () ⇒ newPanel)
    }
  }

  //
  def newPanel: Unit = authenticationSelector.now.get.foreach { f ⇒ setting() = Some(f.buildEmpty) }

  //
  lazy val authenticationTable = {

    case class Reactive(testedAuth: TestedAuthentication) {

      def toLabel(message: String, test: Test) =
        label(
          message,
          scalatags.JsDom.all.marginLeft := 10,
          test match {
            case PassedTest  ⇒ label_success
            case PendingTest ⇒ label_warning
            case _           ⇒ label_danger +++ pointer
          },
          onclick := { () ⇒
            if (!test.passed) {
              panels.stackPanel.content() = test.errorStack.stackTrace
              panels.stackPanel.open
            }
          }
        )

      lazy val render = Rx {
        tr(omsheet.docEntry +++ (lineHeight := "35px"))(
          td(colMD(2))(
            tags.a(testedAuth.auth.data.name, omsheet.docTitleEntry +++ floatLeft +++ omsheet.colorBold("white"), cursor := "pointer", onclick := { () ⇒
              authenticationSelector.now.set(testedAuth.auth.factory)
              setting() = Some(testedAuth.auth)
            })
          ),
          td(colMD(6) +++ sheet.paddingTop(5))(label(testedAuth.auth.data.name, label_primary)),
          td(colMD(2))(
            for {
              t ← testedAuth.tests
            } yield {
              toLabel(t.message, t)
            }
          ),
          td(
            bs.glyphSpan(glyph_trash, () ⇒ removeAuthentication(testedAuth.auth))(omsheet.grey +++ sheet.paddingTop(9) +++ "glyphitem" +++ glyph_trash)
          )
        )
      }
    }

    Rx {
      tags.div(
        setting() match {
          case Some(p: AuthenticationPlugin) ⇒ tags.div(
            authenticationSelector.now.selector,
            div(sheet.paddingTop(20))(p.panel)
          )
          case _ ⇒
            tags.table(width := "100%")(
              for (a ← auths()) yield {
                Seq(Reactive(a).render)
              }
            )
        }
      )
    }
  }

  val newButton = bs.button("New", btn_primary, () ⇒ newPanel)

  val saveButton = bs.button("Save", btn_primary, () ⇒ {
    save
  })

  lazy val dialog: ModalDialog =
    bs.ModalDialog(
      omsheet.panelWidth(52),
      onopen = () ⇒ {
        if (!initialCheck.now) {
          getAuthentications
        }
      },
      onclose = () ⇒ {
        setting() = None
      }
    )

  dialog.header(
    div(height := 55)(
      b("Authentications")
    )
  )

  dialog body (tags.div(authenticationTable))

  dialog.footer(
    tags.div(
      Rx {
        bs.buttonGroup()(
          setting() match {
            case Some(_) ⇒ saveButton
            case _       ⇒ newButton
          },
          ModalDialog.closeButton(dialog, btn_default, "Cancel")
        )
      }
    )
  )

  def removeAuthentication(ad: AuthenticationPlugin) = {
    ad.remove(() ⇒ getAuthentications)
  }

  def save = {
    setting.now.map {
      _.save(() ⇒ {
        getAuthentications
      })
    }
    setting() = None
  }

  def testAuthentications = {
    auths.now.foreach { testedAuth ⇒
      testedAuth.auth.test.foreach { newTest ⇒
        auths() = auths.now.filterNot {
          _ == testedAuth
        } :+ testedAuth.copy(tests = newTest)
      }
    }
    initialCheck() = true
  }

}