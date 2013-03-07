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

import util.{ Failure, Success, Try }
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import com.thoughtworks.xstream.XStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import org.openmole.ide.misc.tools.util.ID
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
import org.openmole.misc.exception.InternalProcessingError

class GUISerializer {

  val xstream = new XStream
  val workDir = Workspace.newDir

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
  xstream.alias("source", classOf[ISourceDataProxyUI])

  def serializeConcept(concept: String, set: List[(_, ID.Type)]) = {
    val conceptDir = new File(workDir, concept)
    conceptDir.mkdirs
    set.foreach {
      case (s, id) ⇒
        new File(conceptDir, id + ".xml").withWriter {
          xstream.createObjectOutputStream(_, concept)
        }
    }
  }

  def serialize(fromFile: String) = {
    serializeConcept("prototype", Proxys.prototypes.map { s ⇒ s -> s.id }.toList)
    serializeConcept("environment", Proxys.environments.map { s ⇒ s -> s.id }.toList)
    serializeConcept("sampling", Proxys.samplings.map { s ⇒ s -> s.id }.toList)
    serializeConcept("hook", Proxys.hooks.map { s ⇒ s -> s.id }.toList)
    serializeConcept("source", Proxys.sources.map { s ⇒ s -> s.id }.toList)
    serializeConcept("taskMap", Proxys.tasks.map { s ⇒ s -> s.id }.toList)
    serializeConcept("mole", ScenesManager.moleScenes.map { ms ⇒ ms -> ms.manager.id }.toList)
    val os = new TarOutputStream(new FileOutputStream(fromFile))
    try os.createDirArchiveWithRelativePathNoVariableContent(workDir)
    finally os.close
    clear
  }

  def readStream(f: File) = Try {
    try xstream.createObjectInputStream(new FileReader(f))
    catch {
      case e: Throwable ⇒
        throw new InternalProcessingError(e, "An error occured when loading " + f.getAbsolutePath + "\n")
    }
  }

  def unserializeProxy(concept: String) =
    new File(workDir, concept).listFiles.toList.flatMap {
      f ⇒
        readStream(f) match {
          case Success(x: ObjectInputStream) ⇒
            try {
              val readObject = x.readObject
              readObject match {
                case ms: BuildMoleScene ⇒ ScenesManager.addBuildSceneContainer(ms)
                case _ ⇒
              }
              None
            } catch {
              case eof: EOFException ⇒ None
              case e: Throwable ⇒
                Some(new InternalProcessingError(e, "Failed to unserialize a data of type " + concept))
            } finally x.close
          case Failure(t) ⇒ Some(t)
        }
    }

  def unserialize(fromFile: String) = {
    StatusBar().clear
    Proxys.clearAll
    ScenesManager.closeAll

    val os = new TarInputStream(new FileInputStream(fromFile))
    os.extractDirArchiveWithRelativePathAndClose(workDir)

    val ret = unserializeProxy("prototype") ++
      unserializeProxy("sampling") ++
      unserializeProxy("environment") ++
      unserializeProxy("hook") ++
      unserializeProxy("source") ++
      unserializeProxy("taskMap") ++
      unserializeProxy("mole")
    clear
    ret
  }

  def clear = {
    workDir.recursiveDelete
    workDir.mkdirs
  }
}
