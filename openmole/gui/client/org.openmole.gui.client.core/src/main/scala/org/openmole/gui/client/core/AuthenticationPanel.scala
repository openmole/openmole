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
import com.raquo.laminar.api.features.unitArrows
import org.openmole.gui.client.core.NotificationManager.toService
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.Selector.Options
import scaladget.bootstrapnative.bsn

object AuthenticationPanel:

  case class TestingAuthentication(auth: AuthenticationPlugin[?], tests: Signal[Seq[Test]], factoryName: String, remove: () => Future[Unit])
  case class DisplayedAuthentication(plugin: AuthenticationPlugin[?], factoryName: String)

  val currentAuthentication: Var[Option[DisplayedAuthentication]] = Var(None)
  val testingAuthentications: Var[Seq[TestingAuthentication]] = Var(Seq())

  def render(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins) =
    given NotificationService = NotificationManager.toService(panels.notifications)

    def refreshAuthentications =
      val tested =
        for
          factory <- plugins.authenticationFactories.sortBy(_.name)
        yield
          factory.getData.map: data =>
            data.map: d =>
              TestingAuthentication(factory.build(d), EventStream.fromFuture(factory.test(d), true).toSignal(Seq()), factory.name, () => factory.remove(d))
          .recover: e =>
            toService(panels.notifications).notify(NotificationLevel.Error,  s"Error while getting authentication", org.openmole.gui.client.ext.ClientUtil.errorTextArea(ErrorData.toStackTrace(e)))
            Seq()

      Future.sequence(tested).map(_.flatten).foreach(testingAuthentications.set)

    def getAuthSelector(factoryName: String) =
      lazy val authenticationSelector: Options[AuthenticationPluginFactory] =

        val currentInd =
          val ind = plugins.authenticationFactories.map { _.name }.indexOf(factoryName)
          if ind == -1 then 0 else ind

        plugins.
          authenticationFactories.options(
            currentInd,
            bsn.btn_warning,
            _.name,
            onclose = () ⇒ currentAuthentication.set:
              authenticationSelector.content.now().map: s =>
                DisplayedAuthentication(s.buildEmpty, s.name)
          )

      authenticationSelector

    def save =
      currentAuthentication.now().foreach: auth =>
        auth.plugin.save.andThen: _ =>
          currentAuthentication.set(None)
          refreshAuthentications

    val newButton = button(
      "New authentication",
      btn_primary,
      marginLeft := "40",
      onClick -->
        currentAuthentication.set:
          plugins.authenticationFactories.headOption.map: factory =>
            DisplayedAuthentication(factory.buildEmpty, factory.name)
    )

    val saveButton = button("Save", btn_primary, onClick --> save)

    val cancelButton =
      button("Cancel", btn_secondary_outline, onClick -->
        currentAuthentication.update: as ⇒
          as match
            case None ⇒ panels.closeExpandable
            case _ ⇒
          None
      )

    case class Reactive(testingAuthentication: TestingAuthentication, i: Int):

      val errorOn = Var(false)
      val currentStack: Var[String] = Var("")

      def toLabel(test: Test) =
        def lab(message: String) = span(
          message,
          cls := "badgeStatus"
        )

        test match
          case t: PassedTest ⇒ lab(t.message).amend(background := "#a5be21")
          case t: FailedTest ⇒ lab(t.message).amend(
            background := "#c8102e", color := "white", cursor.pointer,
            onClick --> { _ ⇒
              currentStack.set(t.error.map(ErrorData.stackTrace).getOrElse(""))
              errorOn.update(!_)
            }
          )

      def columnizer(el: HtmlElement) = div(el, width := "150px")

      def render =
        div(flexRow,
          cls := "docEntry",
          backgroundColor := (if i % 2 == 0 then "#d1dbe4" else "#f4f4f4"),
          a(
            testingAuthentication.auth.name, float.left, color := "#222", width := "350px", cursor.pointer,
            onClick -->
              currentAuthentication.set:
                Some(DisplayedAuthentication(testingAuthentication.auth, testingAuthentication.factoryName))
          ),
          columnizer(
            span(
              cls := "badgeOM",
              testingAuthentication.factoryName)
          ),
          div(
            glyphSpan(glyph_trash).amend(
              cls := "glyphitem", marginLeft := "50px", color := "#222", fontSize := "18",
              onClick --> {
                _ =>
                  testingAuthentication.remove().andThen: _ =>
                    refreshAuthentications
              }
            )
          ),
          div(
            children <--
              testingAuthentication.tests.map: tests =>
                tests.map(t => columnizer(toLabel(t)))
          )
        )



    val authPanel =
      div(marginTop := "50",
        child <-- currentAuthentication.signal.map:
          case Some(p) ⇒
            div(
              padding := "20",
              getAuthSelector(p.factoryName).selector,
              p.plugin.panel.amend(
                div(
                  marginTop := "20",
                  display.flex,
                  justifyContent.flexEnd,
                  div(btnGroup, saveButton, cancelButton)
                )
              )
            )
          case None ⇒
            div(
              children <--
                testingAuthentications.signal.map: as ⇒
                  as.zipWithIndex.map: (a, i) ⇒
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
            )
      )

    div(
      div(margin := "20px", flexRow, alignItems.center,
        div(cls := "close-button bi-x", backgroundColor := "#bdadc4", borderRadius := "20px", onClick --> { _ ⇒ panels.closeExpandable }),
        newButton
      ),
      authPanel,
      onMountCallback(_ => refreshAuthentications)
    )


