//package scaladget.tools
//
///*
// * Copyright (C) 14/03/17 // mathieu.leclaire@openmole.org
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//import org.scalajs.dom
//import org.scalajs.dom.{Element, Event}
//
//object Utils extends utils.Utils
//
//package object utils {
//
//  trait Utils {
//
//    implicit class ElementListener(element: Element) {
//
//      def onClickOutside(f: () => Unit) = {
//        dom.document.addEventListener("mousedown", (e: Event) => {
//          f()
//        })
//      }
//    }
//
//    type ID = String
//
//    def uuID: ID = scala.util.Random.alphanumeric.take(10).mkString
//
//    implicit class ShortID(id: ID) {
//      def short: String = id.take(5)
//
//      def short(prefix: String): String = s"$prefix$short"
//    }
//
//  }
//
//}