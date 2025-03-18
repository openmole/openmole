package org.openmole.gui.client.tool

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement.isActive
import com.raquo.laminar.api.features.unitArrows
import scaladget.bootstrapnative.bsn.{btn_secondary_string, btn_success_string}

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


  object ExclusiveButtons:
    def apply(
     buttons: Seq[String],
     activeStateClass: String = btn_success_string,
     unactiveStateClass: String = btn_secondary_string,
     initial: Int = 0) = new ExclusiveButtons(buttons, activeStateClass, unactiveStateClass, Var(initial))

  case class ExclusiveButtons(buttons: Seq[String], activeStateClass: String, unactiveStateClass: String, selected: Var[Int]):

    import scaladget.bootstrapnative.*

    lazy val element =
      div(
        bsnsheet.btnGroup, bsnsheet.btnGroupToggle, dataAttr("toggle") := "buttons",
        children <--
          selected.signal.distinct.map: s =>
            buttons.zipWithIndex.map: (rb, index) =>
              val isSelected = s == index

              label(
                cls := s"btn ${if isSelected then activeStateClass else  unactiveStateClass}",
                cls.toggle("focus active") := isSelected,
                input(`type` := "radio"/*, name := "options"*/, idAttr := s"option${index + 1}", checked := isSelected),
                rb.text,
                onClick --> selected.set(index)
              )
      )

