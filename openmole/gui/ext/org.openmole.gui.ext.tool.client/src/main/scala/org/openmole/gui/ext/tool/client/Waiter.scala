package org.openmole.gui.ext.tool.client

import org.openmole.gui.ext.data._
import org.scalajs.dom.raw.HTMLElement
import rx._
import scaladget.tools._
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
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
    div(ms("spinner-wave"))(
      tags.div(), tags.div(), tags.div()
    )
}

import org.openmole.gui.ext.tool.client.Waiter._

class ProcessStateWaiter(processingState: Var[ProcessState]) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  def withTransferWaiter[T <: HTMLElement](f: ProcessState ⇒ TypedTag[T]): TypedTag[HTMLElement] = {

    div(
      Rx {
        val ratio = processingState().ratio
        val waiterSpan = div(
          waiter,
          if (ratio == 0 || ratio == 100) span()
          else span(ms("spinner-wave-ratio"))(ratio + " %")
        )

        processingState() match {
          case x @ (Processing(_) | Finalizing(_, _)) ⇒ waiterSpan
          case y @ (Processed(_))                     ⇒ f(processingState())
          case _                                      ⇒ div()
        }
      }
    )
  }
}

class FutureWaiter[S](waitingForFuture: Future[S]) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  def withFutureWaiter[T <: HTMLElement](
    waitingString:    String,
    onsuccessElement: S ⇒ TypedTag[HTMLElement]
  ): TypedTag[HTMLElement] = {

    val processing: Var[TypedTag[HTMLElement]] = Var(waiter)
    waitingForFuture.andThen {
      case Success(s) ⇒ processing() = onsuccessElement(s)
      case Failure(_) ⇒ processing() = tags.div("Failed to load data")
    }

    div(
      Rx {
        processing()
      }
    )
  }

  def withFutureWaiterAndSideEffect[T <: HTMLElement](
    waitingString:    String,
    onsuccessElement: S ⇒ Any
  ): TypedTag[HTMLElement] = {

    val processing: Var[TypedTag[HTMLElement]] = Var(waiter)
    waitingForFuture.andThen {
      case Success(s) ⇒
        onsuccessElement(s)
        processing() = div()
      case Failure(_) ⇒ processing() = tags.div("Failed to load data")
    }

    div(
      Rx {
        processing()
      }
    )
  }

}