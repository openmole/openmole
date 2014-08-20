package org.openmole.gui.client

import java.util.UUID

import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import scala.concurrent.Future
import scalatags.JsDom._
import all._
import tags2.section
import rx._
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.scalajs.dom.extensions.Ajax
import scala.Some
import org.openmole.gui.shared._
import upickle._
import autowire._
import org.openmole.gui.tools.js.JsRxTags._

@JSExport
object Client {

  val helloValue = Var(0)

  @JSExport
  def run() {
    val submitButton = button("Click meee")(
      cursor := "pointer",
      onclick := { () ⇒
        Post[Api].hello(5).call().foreach { i ⇒
          helloValue() = helloValue() + i
        }
        false
      }
    ).render

    Rx {
      dom.document.body.appendChild(submitButton)
      dom.document.body.appendChild(h1(helloValue).render)
    }
  }
}

object Post extends autowire.Client[String, upickle.Reader, upickle.Writer] {

  override def doCall(req: Request): Future[String] = {
    val url = req.path.mkString("/")
    println(" URL " + url)
    dom.extensions.Ajax.post(
      url = "http://localhost:8080/" + url,
      data = upickle.write(req.args)
    ).map {
        _.responseText
      }
  }

  def read[Result: upickle.Reader](p: String) = upickle.read[Result](p)

  def write[Result: upickle.Writer](r: Result) = upickle.write(r)
}
