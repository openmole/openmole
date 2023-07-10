package org.openmole.gui.client.core

import scaladget.tools._
import org.openmole.gui.shared.data.*
import org.scalajs.dom.raw.HTMLElement
import com.raquo.laminar.api.L._
import org.openmole.gui.client.ext

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.concurrent.Future
import scala.util.{ Failure, Success }

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
    div(cls := "spinner-wave", div(), div(), div())
}

import Waiter._

class ProcessStateWaiter(processingState: Var[ProcessState]) {

  def withTransferWaiter[T <: HTMLElement](f: ProcessState ⇒ HtmlElement): HtmlElement = {

    div(
      child <-- processingState.signal.map { pState ⇒
        val ratio = pState.ratio
        val waiterSpan = div(
          ext.flexColumn, alignItems.center,
          waiter,
          if (ratio == 0 || ratio == 100) span()
          else span(cls := "spinner-wave-ratio", ratio + " %")
        )

        pState match {
          case x @ (Processing(_) | Finalizing(_, _)) ⇒ waiterSpan
          case y @ (Processed(_))                     ⇒ f(pState)
          case _                                      ⇒ div()
        }
      }
    )
  }
}

class FutureWaiter[S](waitingForFuture: Future[S]) {

  def withFutureWaiter[T <: HTMLElement](
    waitingString:    String,
    onsuccessElement: S ⇒ HtmlElement
  ): HtmlElement = {

    val processing: Var[HtmlElement] = Var(waiter)
    waitingForFuture.andThen {
      case Success(s) ⇒ processing.set(onsuccessElement(s))
      case Failure(_) ⇒ processing.set(div("Failed to load data"))
    }

    div(
      child <-- processing.signal
    )
  }

  def withFutureWaiterAndSideEffect[T <: HTMLElement](
    waitingString:    String,
    onsuccessElement: S ⇒ Any
  ): HtmlElement = {

    val processing: Var[HtmlElement] = Var(waiter)
    waitingForFuture.andThen {
      case Success(s) ⇒
        onsuccessElement(s)
        processing.set(div())
      case Failure(_) ⇒ processing.set(div("Failed to load data"))
    }

    div(child <-- processing.signal)
  }

}