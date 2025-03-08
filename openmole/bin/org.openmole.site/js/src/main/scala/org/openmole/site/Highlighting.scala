package org.openmole.site

import scaladget.highlightjs.HighlightJS

import scala.scalajs.js
import js.Dynamic.{ literal => lit }

/*
 * Copyright (C) 12/04/17 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

object Highlighting {

  var METHOD = lit(
    "className" → js.RegExp("function"),
    "beginKeywords" → js.RegExp("def"),
    "end" → js.RegExp("""/[:={\[(\n;]/"""),
    "excludeEnd" → "true",
    "contains" → js.Array("NAME")
  )

  def openmoleGrammar() = lit(
    "keywords" → lit(
      "literal" → js.RegExp("true false null"),
      "keyword" → js.RegExp("type yield lazy override def with val var sealed abstract private trait object if forSome for while throw finally protected extends import final return else break new catch super class case package default try this match continue throws implicit")
    ),
    "contains" → js.Array(METHOD) /*,
    contains: [
      hljs.C_LINE_COMMENT_MODE,
      hljs.C_BLOCK_COMMENT_MODE,
      STRING,
      SYMBOL,
      TYPE,
      METHOD,
      CLASS,
      hljs.C_NUMBER_MODE,
      ANNOTATION
    ]*/
  )

  //  println("RE " + HighlightJSConstants.BACKSLASH_ESCAPE)
  //  HighlightJS.registerLanguage("openmole", openmoleGrammar(new HighlightJS))
  //  println("opopo " + HighlightJS.getLanguage("openmole"))
  //  println(HighlightJS.listLanguages)
  //  println("grammar " + openmoleGrammar)

  def init = {
    HighlightJS.initHighlighting()
  }
}
