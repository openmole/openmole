package org.openmole.gui.client.core

import fr.iscpif.scaladget.api.BootstrapTags._
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import fr.iscpif.scaladget.tools.JsRxTags._
import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.HTMLElement
import rx._

import scalatags.JsDom.all._
import scalatags.JsDom.{TypedTag, tags}

/*
 * Copyright (C) 22/12/15 // mathieu.leclaire@openmole.org
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

object ClientProcessState {
  implicit def fTStateToClientTState(f: Var[ProcessState]): ClientProcessState = new ClientProcessState(f)
}

class ClientProcessState(processingState: Var[ProcessState]) {

  def withWaiter[T <: HTMLElement](f: ProcessState => TypedTag[T]): TypedTag[HTMLElement] = {

    val waiter =
      bs.div("spinner-wave")(
        tags.div(), tags.div(), tags.div()
      )

    tags.div(
      Rx {
        val ratio = processingState().ratio
        val waiterSpan = tags.div(waiter,
          if (ratio == 0 || ratio == 100) tags.span() else bs.span("spinner-wave-ratio")(ratio + " %")
        )

        processingState() match {
          case x @ (Processing(_) | Finalizing(_, _)) ⇒ waiterSpan
          case y @ (Processed(_)) ⇒ f(processingState())
          case _=> tags.div()
        }
      }
    )
  }

}