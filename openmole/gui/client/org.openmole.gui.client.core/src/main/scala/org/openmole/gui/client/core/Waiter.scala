package org.openmole.gui.client.core

import fr.iscpif.scaladget.api.BootstrapTags._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.tools.JsRxTags._
import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.HTMLElement
import rx._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scala.concurrent.Future
import scala.util.{ Success, Failure }
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }

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

object Waiter {
  implicit def fTStateToProcessStateWaiter(f: Var[ProcessState]): ProcessStateWaiter = new ProcessStateWaiter(f)

  implicit def fTStateToClientTState[S](f: Future[S]): FutureWaiter[S] = new FutureWaiter(f)

  val waiter =
    bs.div("spinner-wave")(
      tags.div(), tags.div(), tags.div()
    )
}

import Waiter._

class ProcessStateWaiter(processingState: Var[ProcessState]) {

  def withTransferWaiter[T <: HTMLElement](f: ProcessState ⇒ TypedTag[T]): TypedTag[HTMLElement] = {

    tags.div(
      Rx {
        val ratio = processingState().ratio
        val waiterSpan = tags.div(
          waiter,
          if (ratio == 0 || ratio == 100) tags.span() else bs.span("spinner-wave-ratio")(ratio + " %")
        )

        processingState() match {
          case x @ (Processing(_) | Finalizing(_, _)) ⇒ waiterSpan
          case y @ (Processed(_))                     ⇒ f(processingState())
          case _                                      ⇒ tags.div()
        }
      }
    )
  }
}

class FutureWaiter[S](waitingForFuture: Future[S]) {

  def withFutureWaiter[T <: HTMLElement](f: Future[S] ⇒ TypedTag[T])(
    waitingString: String,
    onsuccess:     (S) ⇒ Unit = s ⇒ {},
    onfailure:     () ⇒ Unit  = () ⇒ {}
  ): TypedTag[HTMLElement] = {

    val processing = Var(false)
    waitingForFuture.andThen {
      case Success(s) ⇒
        onsuccess(s)
        processing() = false
      case Failure(_) ⇒
        onfailure()
        processing() = false
    }

    tags.div(
      Rx {
        val waiterSpan = tags.div(
          waiter,
          if (processing() == false) tags.span() else bs.span("spinner-wave-ratio")
        )

      }
    )
  }
}