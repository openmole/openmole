package org.openmole.gui.client.tool

import com.raquo.laminar.api.L._
import scaladget.bootstrapnative.bsn._

class TagBadge {
  val tags: Var[Seq[Span]] = Var(Seq())

  private def buildTag(text: String) = {
    span(text,
      badge_secondary, margin := "3px", fontSize := "14px", padding := "7px", display.flex, flexDirection.row, alignItems.center,
      inContext(thisNode =>
        span(cls := "bi-x", paddingLeft := "10", cursor.pointer,
          onClick --> { _ => tags.update { t => t.filterNot(_ == thisNode) } })
      )
    )
  }

  private val tagTextInput =
    inputTag("").amend(placeholder := "your tag here", width := "200px", height := "40px")

  def render(initialTags: Seq[String]) = {
    tags.set(initialTags.map{buildTag})
    form(display.flex, flexDirection.row,
      tagTextInput,
      onSubmit.preventDefault --> { _ =>
        val in = tagTextInput.ref.value
        if (!in.isEmpty) {
          tags.update { ts => ts :+ buildTag(in) }
          tagTextInput.ref.value = ""
        }
      },
      div(display.flex, flexDirection.row, flexWrap.wrap, width := "100%", children <-- tags.signal)
    )
  }
}
