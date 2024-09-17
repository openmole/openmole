package org.openmole.site.content

/*
 * Copyright (C) 2023 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.site.content.header.*

def Template = PageContent(html"""

See CheatsheetScalatex.txt for a reminder of the main Scalatex features needed in the OpenMOLE doc.



${h2{"My header, top level"}}

Some text to explain something.
Another wonderful sentence with a word in ${b{"bold"}} and one in ${i{"italic"}}.

$br

A new paragraph opening for a code example:

${hl.openmole("""
  // Define variables i and j of type Int
  val i = Val[Int]
  val j = Val[Int]""")}

Some text after the code bit.


${h3{"A header with a lower level"}}
Some very interesting text with some ${code{"inline code"}}.



${h2{"A second top level header"}}

Text opening on an unordered list:
 ${ul(
   li{"something,"},
   li{"that needs,"},
   li{"to be enumerated."}
 )}

""")
