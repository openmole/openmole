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
import fr.iscpif.scaladget.api.Selector.Options

class AuthenticationPanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  case class TestedAuthentication(auth: AuthenticationPlugin, tests: Seq[Test])

  val authSetting: Var[Option[AuthenticationPlugin]] = Var(None)
  private lazy val auths: Var[Seq[TestedAuthentication]] = Var(Seq())
  lazy val initialCheck = Var(false)

  def getAuthSelector(currentFactory: AuthenticationPluginFactory) = {
    lazy val authenticationSelector: Options[AuthenticationPluginFactory] = {
      val factories = Plugins.authenticationFactories.now
      val currentInd = {
        val ind = factories.map { _.name }.indexOf(currentFactory.name)
        if (ind == -1) 0 else ind
      }

      factories.options(currentInd, btn_primary, (a: AuthenticationPluginFactory) ⇒ a.name, onclose = () ⇒
        authSetting() = authenticationSelector.content.now.map {
          _.buildEmpty
        })
    }
    authenticationSelector
  }

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

  lazy val authenticationTable = {

    case class Reactive(testedAuth: TestedAuthentication) {
      def toLabel(test: Test) = {
        val lab = label(
          test.message,
          scalatags.JsDom.all.marginLeft := 10
        )
        test match {
          case PassedTest(_) ⇒ lab(label_success)
          case PendingTest   ⇒ lab(label_warning)
          //FIXME: a bug seems to prevent from opening a popover from a dialog
          case _             ⇒ lab(label_danger).popover(test.errorStack.stackTrace, title = Some("Errors"))
        }
      }

      lazy val render = Rx {
        tr(omsheet.docEntry +++ (lineHeight := "35px"))(
          td(colMD(2))(
            tags.a(testedAuth.auth.data.name, omsheet.docTitleEntry +++ floatLeft +++ omsheet.colorBold("white"), cursor := "pointer", onclick := { () ⇒
              authSetting() = Some(testedAuth.auth)
            })
          ),
          td(colMD(6) +++ sheet.paddingTop(5))(label(testedAuth.auth.factory.name, label_primary)),
          td(colMD(2))(
            for {
              t ← testedAuth.tests
            } yield {
              toLabel(t)
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
        authSetting() match {
          case Some(p: AuthenticationPlugin) ⇒ div(sheet.paddingTop(20))(p.panel)
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

  val newButton = bs.button("New", btn_primary, () ⇒ authSetting() = Plugins.authenticationFactories.now.headOption.map {
    _.buildEmpty
  })

  val saveButton = bs.button("Save", btn_primary, () ⇒ {
    save
  })

  val cancelButton = bs.button("Cancel", btn_default, () ⇒ {
    authSetting.now match {
      case None ⇒ dialog.hide
      case _    ⇒ authSetting() = None
    }
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
        authSetting() = None
      }
    )

  dialog.header(
    div(
      Rx {
        div(
          authSetting() match {
            case Some(o) ⇒ getAuthSelector(o.factory).selector
            case _       ⇒ b("Authentications")
          }
        )
      }
    )
  )

  dialog body (tags.div(authenticationTable))

  dialog.footer(
    tags.div(
      Rx {
        bs.buttonGroup()(
          authSetting() match {
            case Some(_) ⇒ saveButton
            case _       ⇒ newButton
          },
          cancelButton
        )
      }
    )
  )

  def removeAuthentication(ad: AuthenticationPlugin) = {
    ad.remove(() ⇒ getAuthentications)
  }

  def save = {
    authSetting.now.map {
      _.save(() ⇒ {
        getAuthentications
      })
    }
    authSetting() = None
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