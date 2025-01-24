package org.openmole.gui.client.tool

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement.isActive

object Component:

  class Switch(name: String, activated: Boolean, cl: String = "", onChange: Boolean => Unit = identity):
    val checked = Var[Boolean](activated)

    private val in: Input = 
      input(
        `type` := "checkbox", 
        L.checked := activated,
        onClick.mapToChecked --> checked
      )

    val element = 
      div(display.flex, flexDirection.row, cls := cl,
        div(name, height := "34px", marginRight := "10px", display.flex, flexDirection.row, alignItems.center),
        label(cls := "switch",
          in,
          span(cls := "slider round")
        ),
        checked.toObservable --> Observer[Boolean](onChange)
      )

