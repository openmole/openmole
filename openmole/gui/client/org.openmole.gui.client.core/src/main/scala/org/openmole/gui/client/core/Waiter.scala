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

  def waiter(foregroundColor: String = "black") = 
    div(cls := "spinner-wave", div(backgroundColor := foregroundColor), div(backgroundColor := foregroundColor), div(backgroundColor := foregroundColor))


  def doOrWait(actionDiv: Div, action: () => Option[Future[_]], onSuccess: ()=> Unit, waiter: Div = Waiter.waiter()) = 
    val processing: Var[Boolean] = Var(false)
    div(
      child <-- processing.signal.map: p=> 
        p match
          case true=> waiter
          case false=> actionDiv.amend(
            onClick --> { _=>
              processing.set(true)
              action().foreach: a=> 
                a.foreach: _ =>
                  processing.set(false)
                  onSuccess()
            }
          )
    )
}

import Waiter._

class ProcessStateWaiter(processingState: Var[ProcessState]) {

  def withTransferWaiter[T <: HTMLElement](foregroundColor: String = "black")(f: ProcessState ⇒ HtmlElement): HtmlElement = {

    div(
      child <-- processingState.signal.map { pState ⇒
        val ratio = pState.ratio
        val waiterSpan = div(
          ext.flexColumn, alignItems.center,
          waiter(foregroundColor),
          if (ratio == 0 || ratio == 100) span()
          else span(color := foregroundColor, ratio + " %")
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
    onsuccessElement: HtmlElement,
    foregroundColor: String = "black"
  ): HtmlElement = {

    val processing: Var[HtmlElement] = Var(waiter(foregroundColor))
    waitingForFuture.andThen {
      case Success(s) ⇒ processing.set(onsuccessElement)
      case Failure(_) ⇒ processing.set(div("Failed to load data"))
    }

    div(
      child <-- processing.signal
    )
  }

  def withFutureWaiterAndSideEffect[T <: HTMLElement](
    waitingString:    String,
    onsuccessElement: S ⇒ Any,
    foregroundColor: String = "black"
  ): HtmlElement = {

    val processing: Var[HtmlElement] = Var(waiter(foregroundColor))
    waitingForFuture.andThen {
      case Success(s) ⇒
        onsuccessElement(s)
        processing.set(div())
      case Failure(_) ⇒ processing.set(div("Failed to load data"))
    }

    div(child <-- processing.signal)
  }

}