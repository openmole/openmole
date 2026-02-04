package org.openmole.gui.client.tool.bootstrapnative

/*
 * Copyright (C) 13/01/15 // mathieu.leclaire@openmole.org
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

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom.raw.Event
import bsn.*
import org.openmole.gui.client.tool.randomId

object Selector {

  def options[T](contents: Seq[T],
                 defaultIndex: Int = 0,
                 key: HESetters = emptySetters,
                 naming: T => String,
                 onclose: () => Unit = () => {},
                 onclickExtra: () => Unit = () => {},
                 decorations: Map[T, HESetters] = Map(),
                 fixedTitle: Option[String] = None) = new Options(contents, defaultIndex, key, naming, onclose, onclickExtra, decorations, fixedTitle)


  class Options[T](private val _contents: Seq[T],
                   defaultIndex: Int = 0,
                   key: HESetters = emptySetters,
                   naming: T => String,
                   onclose: () => Unit,
                   onclickExtra: () => Unit = () => {},
                   decorations: Map[T, HESetters],
                   fixedTitle: Option[String]) {

    implicit def tToString(t: T): String = naming(t)

    val contents = Var(_contents)
    val opened = Var(false)
    val autoID = randomId

    val content: Var[Option[T]] = Var(_contents.size match {
      case 0 ⇒ None
      case _ ⇒
        if (defaultIndex < _contents.size) Some(_contents(defaultIndex))
        else _contents.headOption
    })

    def setContents(cts: Seq[T], onset: () ⇒ Unit = () ⇒ {}) = {
      contents.set(cts)
      content.set(cts.headOption)
      onset()
    }

    def emptyContents = {
      contents.set(Seq())
      content.set(None)
    }

    def set(t: T) = content.set(Some(t))

    def get: Option[T] = content.now()

    def getOrElse(t: T) = get.getOrElse(t)

    def isContentsEmpty = contents.now().isEmpty

    def close = opened.set(false)

    lazy val selector =
      div(
        cls := "dropdown",
        cls.toggle("show") <-- opened.signal,
        button(idAttr := autoID,
          `type` := "button",
          dataAttr("toggle") := "dropdown",
          key,
          aria.hasPopup := true,
          aria.expanded <-- opened.signal,
          child.text <-- content.signal.map { c => fixedTitle.getOrElse(c.map(naming).getOrElse("")) },
          span(caret),
          onClick --> { _ =>
            opened.set(!opened.now())
            onclickExtra()
          }
        ),
        div(
          dropdownMenu,
          cls.toggle("show") <-- opened.signal,
          aria.labelledBy(autoID),
          for {
            c <- contents.now()
          } yield {
            a(
              cls := "dropdown-item",
              href := "#",
              decorations.getOrElse(c, emptySetters).toSeq,
              s" ${naming(c)}",
              onClick --> { _ =>
                content.set(Some(c))
                opened.update(!_)
                onclose()
              }
            )
          }
        )
      )

    selector.ref.addEventListener("mousedown", (e: Event) => {
      e.stopPropagation()
    })


    // FIXME May Leak
    implicit class ElementListener(element: org.scalajs.dom.Element):
      def onClickOutside(f: () => Unit) =
        org.scalajs.dom.document.addEventListener(
          "mousedown",
          (e: org.scalajs.dom.Event) => f()
        )

    selector.ref.onClickOutside(() => close)
  }


}

