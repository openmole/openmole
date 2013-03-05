/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

import scala.util.Try
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import com.thoughtworks.xstream.XStream
import java.io.EOFException
import com.thoughtworks.xstream.io.xml.DomDriver
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.implementation.dataproxy._
import java.io.ObjectInputStream
import java.nio.file.Files
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.TarArchiver._
import com.thoughtworks.xstream.io.HierarchicalStreamWriter

object GUISerializer {
  def serializePrefix(path: File) = path.getParentFile + "/" + path.getName.split('.')(0)
}
import GUISerializer._
class GUISerializer {

  val tmpDir = (Workspace.newFile)
  val path = tmpDir.getCanonicalPath

  val extractDir = Files.createTempDirectory("openmole").toFile
  val extractPath = extractDir.getAbsolutePath

  val xstream = new XStream(new DomDriver)
  val taskConverter = new TaskConverter(xstream.getMapper,
    xstream.getReflectionProvider,
    this,
    new SerializerState)
  val prototypeConverter = new PrototypeConverter(xstream.getMapper,
    xstream.getReflectionProvider,
    this,
    new SerializerState)
  val samplingConverter = new SamplingCompositionConverter(xstream.getMapper,
    xstream.getReflectionProvider,
    this,
    new SerializerState)
  val environmentConverter = new EnvironmentConverter(xstream.getMapper,
    xstream.getReflectionProvider,
    this,
    new SerializerState)
  val hookConverter = new HookConverter(xstream.getMapper,
    xstream.getReflectionProvider,
    this,
    new SerializerState)

  xstream.registerConverter(new MoleSceneConverter(this))
  xstream.registerConverter(taskConverter)
  xstream.registerConverter(prototypeConverter)
  xstream.registerConverter(samplingConverter)
  xstream.registerConverter(environmentConverter)
  xstream.registerConverter(hookConverter)

  xstream.alias("molescene", classOf[MoleScene])
  xstream.alias("taskMap", classOf[ITaskDataProxyUI])
  xstream.alias("sampling", classOf[ISamplingCompositionDataProxyUI])
  xstream.alias("prototype", classOf[IPrototypeDataProxyUI])
  xstream.alias("environment", classOf[IEnvironmentDataProxyUI])
  xstream.alias("hook", classOf[IHookDataProxyUI])

  def serializeConcept(concept: String,
                       set: List[(_, Int)]) = {
    val conceptFile = new File(path + "/" + concept)
    conceptFile.mkdirs
    set.foreach {
      case (s, id) ⇒
        val f = new File(conceptFile.getCanonicalFile + "/" + id + ".xml")
        val writer = new FileWriter(f)
        val out = xstream.createObjectOutputStream(writer, concept)
        out.writeObject(s)
        out.close
    }
  }

  def serialize(fromFile: String) = {
    if (tmpDir.getParentFile.isDirectory) {
      serializeConcept("prototype", Proxys.prototypes.map { s ⇒ s -> s.id }.toList)
      serializeConcept("environment", Proxys.environments.map { s ⇒ s -> s.id }.toList)
      serializeConcept("sampling", Proxys.samplings.map { s ⇒ s -> s.id }.toList)
      serializeConcept("hook", Proxys.hooks.map { s ⇒ s -> s.id }.toList)
      serializeConcept("taskMap", Proxys.tasks.map { s ⇒ s -> s.id }.toList)
      serializeConcept("mole", ScenesManager.moleScenes.map { ms ⇒ ms -> ms.manager.id }.toList)
      val os = new TarOutputStream(new FileOutputStream(fromFile))
      try os.createDirArchiveWithRelativePathNoVariableContent(tmpDir)
      finally os.close
      new File(serializePrefix(tmpDir)).recursiveDelete
    }
  }

  def readStream(f: File) = try {
    Right(xstream.createObjectInputStream(new FileReader(f)))
  } catch {
    case e: Throwable ⇒
      StatusBar().block("An error occured when loading " + f.getAbsolutePath + "\n" + e.getMessage,
        stack = e.getStackTraceString)
      Left
  }

  def unserializeProxy(concept: String) = {
    new File(extractPath + "/" + concept).listFiles.toList.foreach { f ⇒
      readStream(f) match {
        case Right(x: ObjectInputStream) ⇒
          try {
            val readObject = x.readObject
            readObject match {
              case ms: BuildMoleScene ⇒ ScenesManager.addBuildSceneContainer(ms)
              case _ ⇒
            }
          } catch {
            case eof: EOFException ⇒ StatusBar().inform("Project loaded")
            case e: Throwable ⇒ StatusBar().block("Failed to unserialize a data of type " + concept,
              stack = e.getMessage + "\n" + e.getStackTraceString)
          } finally {
            x.close
          }
      }
    }
  }

  def unserialize(fromFile: String) = {
    StatusBar().clear
    Proxys.clearAll
    ScenesManager.closeAll

    val os = new TarInputStream(new FileInputStream(fromFile))
    os.extractDirArchiveWithRelativePathAndClose(extractDir)
    unserializeProxy("prototype")
    unserializeProxy("sampling")
    unserializeProxy("environment")
    unserializeProxy("hook")
    unserializeProxy("taskMap")
    unserializeProxy("mole")
  }
}
