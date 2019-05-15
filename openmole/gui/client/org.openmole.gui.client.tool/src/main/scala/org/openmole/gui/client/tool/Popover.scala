package org.openmole.gui.client.tool

import org.scalajs.dom.raw.{ Event, HTMLElement }
import scaladget.bootstrapnative.Popup.{ Manual, PopupPosition }
import scalatags.JsDom.TypedTag
import scaladget.bootstrapnative.bsn._

object Popover {

  def apply(element: TypedTag[_ <: HTMLElement], topPosition: Double, popContent: TypedTag[_ <: HTMLElement], position: PopupPosition) = {
    lazy val pop1 = element.popover(
      popContent,
      position,
      Manual
    )

    lazy val pop1Render = pop1.render

    pop1Render.onclick = { (e: Event) ⇒
      scaladget.bootstrapnative.bsn.Popover.hide
      scaladget.bootstrapnative.bsn.Popover.toggle(pop1)
      e.stopPropagation
    }
    pop1Render
  }
}
