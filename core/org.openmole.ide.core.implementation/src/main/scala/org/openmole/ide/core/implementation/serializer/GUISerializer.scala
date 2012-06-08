/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.serializer

import com.thoughtworks.xstream.XStream
import java.io.EOFException
import com.thoughtworks.xstream.io.xml.DomDriver
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.dataproxy._
import java.io.ObjectInputStream
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.workflow.MoleScene

object GUISerializer {

  val xstream = new XStream(new DomDriver)
  xstream.registerConverter(new MoleSceneConverter)

  xstream.alias("molescene", classOf[MoleScene])
  xstream.alias("data_proxy", classOf[IDataProxyUI])

  def serialize(toFile: String) = {
    val f = new File(toFile)
    if (f.getParentFile.isDirectory) {
      val writer = new FileWriter(f)

      //root node
      val out = xstream.createObjectOutputStream(writer, "openmole")

      out.writeObject(new SerializedProxys(Proxys.tasks.toSet,
        Proxys.prototypes.toSet,
        Proxys.samplings.toSet,
        Proxys.environments.toSet,
        Proxys.incr.get + 1))
      //molescenes
      ScenesManager.moleScenes.foreach(ms ⇒
        ms match {
          case x: BuildMoleScene ⇒ out.writeObject(x)
          case _ ⇒
        })
      out.close
    }
  }

  def unserialize(fromFile: String) = {
    StatusBar.clear
    val reader = new FileReader(new File(fromFile))
    val in = try {
      Right(xstream.createObjectInputStream(reader))
    } catch {
      case e ⇒
        StatusBar.block("An error occured when loading the project " + fromFile + ". " + e.getMessage,
          stack = e.getStackTraceString)
        Left
    }

    in match {
      case Right(x: ObjectInputStream) ⇒
        try {
          Proxys.clearAll
          ScenesManager.closeAll

          while (true) {
            val readObject = x.readObject
            readObject match {
              case x: SerializedProxys ⇒ x.loadProxys
              case x: BuildMoleScene ⇒ { ScenesManager.addBuildSceneContainer(x) }
              case _ ⇒ throw new UserBadDataError("Failed to unserialize object " + readObject.toString)
            }
          }
        } catch {
          case eof: EOFException ⇒ StatusBar.inform("Project loaded")
          case e ⇒ StatusBar.block("An error occured when loading the project " + fromFile,
            stack = e.getMessage + "\n" + e.getStackTraceString)
        } finally {
          x.close
        }
      case Left ⇒
    }
  }
}