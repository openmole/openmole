package org.openmole.gui.client.tool.ace

import com.raquo.laminar.api.L.*
import org.openmole.gui.client.tool
import org.scalajs.dom

import scala.scalajs.js

object Utils {

  def rangeFor(startRow: Int, startCol: Int, endRow: Int, endCol: Int) =
    js.Dynamic.newInstance(tool.ace.ace.require("ace/range").Range)(startRow, startCol, endRow, endCol).asInstanceOf[Range]

  def getBreakPointElements(editorDiv: HtmlElement) = {
   editorDiv.ref.getElementsByClassName("ace_breakpoint").map {e=>
      e-> e.innerText
    }}
  }
