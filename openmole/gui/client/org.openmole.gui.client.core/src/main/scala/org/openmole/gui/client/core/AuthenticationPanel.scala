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

import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.ext.*

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.shared.data.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.Selector.Options
import scaladget.bootstrapnative.bsn

object AuthenticationPanel {

  case class TestedAuthentication(auth: AuthenticationPlugin, tests: Future[Seq[Test]])

  val authSetting: Var[Option[AuthenticationPlugin]] = Var(None)
  private lazy val auths: Var[Seq[TestedAuthentication]] = Var(Seq())
  lazy val initialCheck = Var(false)


  def render(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins) = {
    given NotificationService = NotificationManager.toService(panels.notifications)

    def getAuthentications =
      plugins.authenticationFactories.map { factory ⇒
        val data = factory.getData
        auths.set(Seq())
        data.foreach {
          _.map { authType =>
            val auth = factory.build(authType)
            auths.update(a => a :+ TestedAuthentication(auth, auth.test))
          }
        }

        initialCheck.set(true)
      }


    def getAuthSelector(currentFactory: AuthenticationPluginFactory) = {
      lazy val authenticationSelector: Options[AuthenticationPluginFactory] = {

        val currentInd = {
          val ind = plugins.authenticationFactories.map {
            _.name
          }.indexOf(currentFactory.name)
          if (ind == -1) 0 else ind
        }

        plugins.authenticationFactories.options(currentInd, bsn.btn_warning, (a: AuthenticationPluginFactory) ⇒ a.name, onclose = () ⇒
          authSetting.set(authenticationSelector.content.now().map {
            _.buildEmpty
          }))
      }
      authenticationSelector
    }

    def removeAuthentication(ad: AuthenticationPlugin) = {
      ad.remove(() ⇒ getAuthentications)
    }

    def save = {
      authSetting.now().map {
        _.save(() ⇒ {
          getAuthentications
        })
      }
      authSetting.set(None)
    }

    val newButton = button(
      "New authentication",
      btn_primary,
      marginLeft := "40",
      onClick --> {
        _ ⇒
          authSetting.set(plugins.authenticationFactories.headOption.map {
            _.buildEmpty
          })
      })

    val saveButton = button("Save", btn_primary, onClick --> {
      _ ⇒ save
    })

    val cancelButton = button("Cancel", btn_secondary_outline, onClick --> {
      _ ⇒ {
        authSetting.update {
          as ⇒
            as match {
              case None ⇒ Panels.closeExpandable
              case _ ⇒
            }
            None
        }
      }
    })

    if (!initialCheck.now()) {
      getAuthentications
    }

    case class Reactive(testedAuth: TestedAuthentication, i: Int) {


      val errorOn = Var(false)
      val currentStack: Var[String] = Var("")

      def toLabel(test: Test) = {
        val lab = span(
          test.message,
          cls := "badgeStatus"
        )

        test match {
          case PassedTest(_) ⇒ lab.amend(background := "#a5be21")
          case PendingTest() ⇒ lab.amend(background := "#f1c345")
          case _ ⇒ lab.amend(
            background := "#c8102e", color := "white", cursor.pointer,
            onClick --> { _ ⇒
              currentStack.set(test.error.map(ErrorData.stackTrace).getOrElse(""))
              errorOn.update(!_)
            }
          )
        }
      }

      val tests: Var[Seq[Test]] = Var(Seq(Test.pending))
      testedAuth.tests.foreach { ts ⇒
        tests.set(ts)
      }

      def columnizer(el: HtmlElement) = div(el, width := "150px")

      lazy val render = {
        div(flexRow,
          cls := "docEntry",
          backgroundColor := {
            if (i % 2 == 0) "#d1dbe4" else "#f4f4f4"
          },
          a(testedAuth.auth.data.name, float.left, color := "#222", width := "350px", cursor.pointer, onClick --> { _ ⇒
            authSetting.set(Some(testedAuth.auth))
          }),
          columnizer(span(
            cls := "badgeOM",
            testedAuth.auth.factory.name)),
          div(
            children <-- tests.signal.map { ts ⇒
              ts.map { t =>
                columnizer(toLabel(t))
              }
            }
          ),
          div(
            glyphSpan(glyph_trash).amend(cls := "glyphitem", marginLeft := "50px", color := "#222", fontSize := "18", onClick --> { _ ⇒ removeAuthentication(testedAuth.auth) }))
        )
      }
    }

    val authPanel = div(marginTop := "50",
      child <-- authSetting.signal.map {
        _ match {
          case Some(p: AuthenticationPlugin) ⇒ div(padding := "20",
            getAuthSelector(p.factory).selector,
            p.panel.amend(
              div(
                marginTop := "20",
                display.flex,
                justifyContent.flexEnd,
                div(btnGroup, saveButton, cancelButton)
              )
            ))
          case _ ⇒
            div(
              children <-- auths.signal.map { as ⇒
                as.zipWithIndex.map { case (a, i) ⇒
                  //for (a ← auths()) yield {
                  val r = Reactive(a, i)
                  div(flexColumn,
                    r.render,
                    r.errorOn.signal.expand(
                      textArea(fontSize := "15px",
                        dropdownError,
                        child.text <-- r.currentStack.signal
                      )
                    )
                  )
                }
              }
            )
        }
      }
    )

    div(
      div(margin := "20px", flexRow, alignItems.center,
        div(cls := "close-button bi-x", backgroundColor := "#bdadc4", borderRadius := "20px", onClick --> { _ ⇒ Panels.closeExpandable }),
        newButton
      ),
      authPanel
    )
  }


}