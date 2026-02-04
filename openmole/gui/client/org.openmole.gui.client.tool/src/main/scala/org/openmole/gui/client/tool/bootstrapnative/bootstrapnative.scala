package org.openmole.gui.client.tool.bootstrapnative

/*
 * Copyright (C) 18/08/16 // mathieu.leclaire@openmole.org
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


import com.raquo.laminar.api.L.{*, given}
import org.openmole.gui.client.tool.bootstrapnative
import bootstrapnative.{BootstrapTags, stylesheet, stylesheet2}

import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal as lit
import scala.scalajs.js.annotation.*

package object bsnsheet extends stylesheet.BootstrapPackage with stylesheet2.Bootstrap2Package

package object bsn extends stylesheet.BootstrapPackage with stylesheet2.Bootstrap2Package with bootstrapnative.BootstrapTags

//@js.native
//@JSImport("bootstrap.native", JSImport.Namespace)
//class Modal(element: Element) extends js.Object {
//
//  def show(): Unit = js.native
//
//  def hide(): Unit = js.native
//}
//@js.native
//@JSImport("bootstrap.native/lib/V3/utils.js", JSImport.Namespace)
//object Utils extends js.Object

//@js.native
//@JSImport("bootstrap.native/src/components/popover-native.js", JSImport.Namespace)
//object Popover extends js.Object {
//  def apply(element: org.scalajs.dom.Element, options: js.Dynamic = lit()): Popover = js.native
//}


object BSN {

  @js.native
  @JSImport("bootstrap.native/src/components/popover-native.js", JSImport.Default)
  class Popover(element: org.scalajs.dom.Element, options: js.Dynamic = lit()) extends js.Object {
    def show(): Unit = js.native

    def hide(): Unit = js.native

    def toggle(): Unit = js.native
  }

  @js.native
  @JSImport("bootstrap.native/src/components/modal-native.js", JSImport.Default)
  class Modal(element: org.scalajs.dom.Element, options: js.Dynamic = lit()) extends js.Object {
    def show(): Unit = js.native

    def hide(): Unit = js.native

    def toggle(): Unit = js.native
  }


  @js.native
  @JSImport("bootstrap.native/src/components/tooltip-native.js", JSImport.Default)
  class Tooltip(element: org.scalajs.dom.Element, options: js.Dynamic = lit()) extends js.Object {

    def show: Unit = js.native

    def hide(): Unit = js.native

    def toggle(): Unit = js.native

    def close(): Unit = js.native
  }

}

//@JSGlobal
//@js.native
//class Collapse(element: Element, options: js.Dynamic = lit()) extends js.Object {
//
//  def show(): Unit = js.native
//
//  def hide(): Unit = js.native
//
//  def toggle(): Unit = js.native
//}