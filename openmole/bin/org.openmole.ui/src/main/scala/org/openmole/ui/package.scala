/**
 * Created by Romain Reuillon on 31/03/16.
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
 *
 */
package org.openmole

import java.net.URI

//package object ui {
//
//  def browse(url: String) = {
//    def browsers =
//      Array(
//        "google-chrome",
//        "chromium-browser",
//        "firefox",
//        "opera",
//        "epiphany",
//        "konqueror",
//        "conkeror",
//        "midori",
//        "kazehakase",
//        "mozilla"
//      )
//
//    try { // attempt to use Desktop library from JDK 1.6+
//      val d = Class.forName("java.awt.Desktop")
//      println(d)
//      val b = d.getDeclaredMethod("browse", classOf[URI])
//      println(b)
//      b.invoke(d.getDeclaredMethod("getDesktop").invoke(null), java.net.URI.create(url))
//      // above code mimicks: java.awt.Desktop.getDesktop().browse()
//    }
//    catch {
//      case ignore: Exception => // library not available or failed
//        println(ignore)
//        val osName = System.getProperty("os.name")
//        if (osName.startsWith("Mac OS")) {
//          Class.forName("com.apple.eio.FileManager")
//            .getDeclaredMethod("openURL", classOf[String])
//            .invoke(null, url)
//        }
//        else if (osName.startsWith("Windows"))
//          Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url)
//        else { // assume Unix or Linux
//          val b =
//            browsers.find { b =>
//              val is = Runtime.getRuntime().exec("which", Array(b)).getInputStream()
//              try is.read() != -1
//              finally is.close
//            }
//
//          b.foreach { b => Runtime.getRuntime().exec(b, Array(url)) }
//        }
//    }
//
//  }
//
//}
