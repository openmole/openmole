package org.openmole.site.content.community

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

object OMCommunity extends PageContent(html"""
The OpenMOLE community has been steadily growing these past years, and we are always happy to welcome new users!

$br$br

Several tools are available to interact with the OpenMOLE community:
${ul(
  li(html"Get help on the ${aa("chat", href := shared.link.chat)}"),
  li(html"Ask more structured questions and get help on the ${aa("forum", href := shared.link.forum)} (it is quite empty yet, but don't be afraid of the void)"),
  li(html"Stay informed about the latest OpenMOLE news via ${aa("Twitter", href := shared.link.twitter)}")
)}

$br

It is also possible to ${aa("report a bug", href := shared.link.issue)} and ${a("help make OpenMOLE better", href := DocumentationPages.howToContribute.file)}.
So many possibilities!

$br

Have a look at the different pages in this section to get to know us better.
""")

