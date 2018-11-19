package org.openmole.gui.client.tool

import rx._
import scalatags.JsDom.all._
import scaladget.tools._

object DynamicScrolledTextArea {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  def apply(content: Rx[String]) = {
    textarea(
      Rx {
        span(
          content()
        )
      }, height := "300px", width := "100%").render
  }
}
