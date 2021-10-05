package org.openmole.gui.client.tool

import org.scalajs.dom.raw.{ Event, HTMLElement }
import scaladget.bootstrapnative.Popup.{ Manual, PopupPosition }
import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._

object Popover {

  def apply(element: HtmlElement, topPosition: Double, popContent: HtmlElement, position: PopupPosition) = {
    lazy val pop1 = element.popover(
      popContent,
      position,
      Manual
    )

    pop1.render.amend(
      onClick --> { (e: Event) ⇒
        //thisNode.hide
        pop1.toggle
        e.stopPropagation
      }
    )
  }
}
