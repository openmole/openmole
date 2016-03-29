/**
 * Created by Romain Reuillon on 29/03/16.
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
package org.openmole.runtime.launcher

import java.io.File
import java.util.ServiceLoader

import org.osgi.framework.Constants
import org.osgi.framework.launch._
import collection.JavaConversions._

import scala.annotation.tailrec

object Launcher {

  def main(args: Array[String]): Unit = {
    case class Config(
      directory: Option[File] = None,
      ignored:   List[String] = Nil,
      args:      List[String] = Nil
    )

    def takeArg(args: List[String]) =
      args match {
        case h :: t ⇒ h
        case Nil    ⇒ ""
      }

    def dropArg(args: List[String]) =
      args match {
        case h :: t ⇒ t
        case Nil    ⇒ Nil
      }

    def takeArgs(args: List[String]) = args.takeWhile(!_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    @tailrec def parse(args: List[String], c: Config = Config()): Config = args match {
      case "-d" :: tail ⇒ parse(tail.tail, c.copy(directory = Some(new File(tail.head))))
      case "--" :: tail ⇒ parse(Nil, c.copy(args = tail))
      case s :: tail    ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
      case Nil          ⇒ c
    }

    val frameworkFactory = ServiceLoader.load(classOf[FrameworkFactory]).iterator().next()

    val osgiConfig = Map[String, String](
      (Constants.FRAMEWORK_STORAGE, ""),
      (Constants.FRAMEWORK_STORAGE_CLEAN, "true")
    )

    val framework = frameworkFactory.newFramework(osgiConfig)
    framework.init()
    val context = framework.getBundleContext

    val config = parse(args.toList)

    val bundles =
      for {
        f ← Option(config.directory.get.listFiles()).getOrElse(Array.empty)
      } yield context.installBundle(f.toURI.toString)

    bundles.foreach { _.start }

    framework.waitForStop(0)
    sys.exit(0)
  }

}
