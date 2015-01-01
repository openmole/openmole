
/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package documentation

object Documentation extends App {

  val site = new scalatex.site.Site {
    def content =
      Map(
        "index.html" -> Index(),
        Pages.console.index -> console.Console(),
        Pages.console.task -> console.Task(),
        Pages.console.java -> console.task.Java(),
        Pages.console.systemExec -> console.task.SystemExec(),
        Pages.console.netlogo -> console.task.NetLogo()
      )
  }
  site.renderTo(args(0) + "/")

}
