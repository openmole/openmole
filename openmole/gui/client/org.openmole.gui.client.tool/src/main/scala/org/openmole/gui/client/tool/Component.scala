package org.openmole.gui.client.tool

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement.isActive

object Component:

  class Switch(name: String, activated: Boolean, cl: String = "", onClickAction: () => Unit = ()=> {}):

    private val in: Input = 
      input(
        `type` := "checkbox", 
        checked := activated, 
        onClick --> {_=> 
          if isChecked 
          then onClickAction()
        }
      )
    val element = 
      div(display.flex, flexDirection.row, cls := cl,
        div(name, height := "34px", marginRight := "10px", display.flex, flexDirection.row, alignItems.center),
        label(cls := "switch",
          in,
          span(cls := "slider round"
          )
        )
      )

    def isChecked = in.ref.checked
