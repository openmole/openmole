package org.openmole.site

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

@js.native
trait HighlightLanguage extends js.Object

object Highlighting {

  val METHOD = lit(
    "className" → js.RegExp("function"),
    "beginKeywords" → js.RegExp("def"),
    "end" → js.RegExp("""/[:={\[(\n;]/"""),
    "excludeEnd" → "true",
    "contains" → js.Array("NAME")
  )

  def openmoleGrammar(hljs: js.Dynamic): js.Dynamic =
    lit(
      "keywords" → lit(
        "literal" → js.RegExp("true false null"),
        "keyword" → js.RegExp(
          "type yield lazy override def with val var sealed abstract private " +
            "trait object if forSome for while throw finally protected extends " +
            "import final return else break new catch super class case package " +
            "default try this match continue throws implicit"
        )
      ),
      "contains" → js.Array(METHOD)
    )

  def init(): Unit = {
    HighlightJS.registerLanguage(
      "openmole",
      ((hljs: js.Dynamic) => openmoleGrammar(hljs)): js.Function
    )

    HighlightJS.highlightAll()
  }

}