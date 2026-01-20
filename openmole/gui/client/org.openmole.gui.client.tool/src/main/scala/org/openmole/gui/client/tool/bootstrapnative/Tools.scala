package org.openmole.gui.client.tool.bootstrapnative

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveElement
import org.openmole.gui.client.tool.bootstrapnative
import org.openmole.gui.client.tool.bootstrapnative.Popup.{Bottom, HoverPopup, PopupPosition, PopupType}
import bsn.*

object Tools {


  case class MyPopoverBuilder(element: ReactiveElement[org.scalajs.dom.Element],
                              popoverElement: HtmlElement,
                              position: PopupPosition = bootstrapnative.Popup.Right,
                              trigger: PopupType = HoverPopup) {

    private val open = Var(false)

    private def openTrigger = open.update(!_)

    def render = {
      element.amend(
        onClick --> { _=> openTrigger},
        open.signal.expand(
        popoverElement.amend(
          onMouseLeave --> { _ => open.set(false) },
        )
      ))

    }
  }
}
