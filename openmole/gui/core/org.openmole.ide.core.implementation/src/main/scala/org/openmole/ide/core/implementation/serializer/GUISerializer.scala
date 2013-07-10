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
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import org.openmole.misc.exception.{ UserBadDataError, ExceptionUtils, InternalProcessingError }
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.{ ErrorWriter, UnmarshallingContext, MarshallingContext }
import collection.mutable
import org.openmole.ide.core.model.workflow.{ IMoleUI, IMoleScene }
import org.openmole.ide.core.model.data.{ ICapsuleDataUI }
import com.thoughtworks.xstream.converters.collections.SingletonCollectionConverter
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import org.openmole.ide.core.model.commons._
import scala.util.Failure
import scala.Some
import org.openmole.ide.core.model.commons.MasterCapsuleType
import scala.util.Success

class GUISerializer { serializer ⇒

  sealed trait SerializationState
  case class Serializing(id: ID.Type) extends SerializationState
  case class Serialized(id: ID.Type) extends SerializationState

  val serializationStates: mutable.HashMap[AnyRef, SerializationState] = mutable.HashMap.empty
  val deserializationStates: mutable.HashMap[ID.Type, AnyRef] = mutable.HashMap.empty

  val xstream = new XStream
  val workDir = Workspace.newDir

  class GUIConverter[T <: AnyRef { def id: ID.Type }](implicit clazz: Manifest[T]) extends ReflectionConverter(xstream.getMapper, xstream.getReflectionProvider) {

    override def marshal(
      o: Object,
      writer: HierarchicalStreamWriter,
      mc: MarshallingContext) = {
      val dataUI = o.asInstanceOf[T]
      serializationStates.get(dataUI) match {
        case None ⇒
          serializationStates += dataUI -> Serializing(dataUI.id)
          marshal(o, writer, mc)
        case Some(Serializing(id)) ⇒
          serializationStates(dataUI) = Serialized(id)
          super.marshal(dataUI, writer, mc)
        case Some(Serialized(id)) ⇒
          writer.addAttribute("id", id.toString)
      }
    }

    override def unmarshal(
      reader: HierarchicalStreamReader,
      uc: UnmarshallingContext) = {
      if (reader.getAttributeCount != 0) {
        val dui = existing(reader.getAttribute("id"))
        dui match {
          case Some(y) ⇒ y
          case _ ⇒
            serializer.deserializeConcept(uc.getRequiredType)
            unmarshal(reader, uc)
        }
      }
      else {
        val o = super.unmarshal(reader, uc)
        o match {
          case y: T ⇒
            existing(y.id) match {
              case None ⇒ add(y)
              case _    ⇒
            }
            y
          case _ ⇒ throw new UserBadDataError("Can't load object " + o)
        }
      }
    }

    override def canConvert(t: Class[_]) = clazz.runtimeClass.isAssignableFrom(t)

    def existing(id: String) = deserializationStates.get(id)
    def add(e: T) = deserializationStates.put(e.id, e)

  }

  val taskConverter = new GUIConverter[ITaskDataProxyUI]
  val prototypeConverter = new GUIConverter[IPrototypeDataProxyUI]
  val samplingConverter = new GUIConverter[ISamplingCompositionDataProxyUI]
  val environmentConverter = new GUIConverter[IEnvironmentDataProxyUI]
  val hookConverter = new GUIConverter[IHookDataProxyUI]
  val sourceConverter = new GUIConverter[ISourceDataProxyUI]
  val capsuleConverter = new GUIConverter[CapsuleData]
  val transitionConverter = new GUIConverter[TransitionData]
  val dataChannelConverter = new GUIConverter[DataChannelData]
  val slotConverter = new GUIConverter[SlotData]

  val optionConverter = new ReflectionConverter(xstream.getMapper, xstream.getReflectionProvider) {

    class CountingHierarchicalStreamReader(reader: HierarchicalStreamReader) extends HierarchicalStreamReader {

      var depth: Int = 0

      def hasMoreChildren = reader.hasMoreChildren

      def moveDown() {
        depth += 1
        reader.moveDown()
      }

      def moveUp() {
        depth -= 1
        reader.moveUp()
      }

      def getNodeName = reader.getNodeName
      def getValue = reader.getValue
      def getAttribute(p1: String) = reader.getAttribute(p1)
      def getAttribute(p1: Int) = reader.getAttribute(p1)
      def getAttributeCount = reader.getAttributeCount
      def getAttributeName(p1: Int) = reader.getAttributeName(p1)
      def getAttributeNames = reader.getAttributeNames
      def appendErrors(p1: ErrorWriter) { reader.appendErrors(p1) }
      def close() { reader.close() }
      def underlyingReader() = reader.underlyingReader()
    }

    override def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = {
      val cReader = new CountingHierarchicalStreamReader(reader)
      Try(super.unmarshal(cReader, context)) match {
        case Success(o) ⇒ o
        case Failure(t) ⇒
          for (i ← 0 until cReader.depth) reader.moveUp
          None
      }
    }

    override def canConvert(t: Class[_]) = classOf[Some[_]].isAssignableFrom(t)
  }

  xstream.registerConverter(taskConverter)
  xstream.registerConverter(prototypeConverter)
  xstream.registerConverter(samplingConverter)
  xstream.registerConverter(environmentConverter)
  xstream.registerConverter(hookConverter)
  xstream.registerConverter(sourceConverter)

  xstream.registerConverter(capsuleConverter)
  xstream.registerConverter(transitionConverter)
  xstream.registerConverter(slotConverter)
  xstream.registerConverter(dataChannelConverter)

  xstream.addImmutableType(classOf[DataChannelData])
  xstream.addImmutableType(classOf[TransitionData])
  xstream.addImmutableType(classOf[SlotData])
  xstream.addImmutableType(classOf[CapsuleData])
  xstream.addImmutableType(classOf[MoleData])

  xstream.alias("DataChannelData", classOf[DataChannelData])
  xstream.alias("MoleData", classOf[MoleData])
  xstream.alias("CapsuleData", classOf[CapsuleData])
  xstream.alias("SlotData", classOf[SlotData])
  xstream.alias("TransitionData", classOf[TransitionData])
  xstream.alias("CapsuleDataUI", classOf[CapsuleDataUI])
  xstream.alias("TaskDataProxyUI", classOf[TaskDataProxyUI])

  xstream.alias("SimpleCapsuleType", SimpleCapsuleType.getClass)
  xstream.alias("StrainerCapsuleType", StrainerCapsuleType.getClass)
  xstream.alias("MasterCapsuleType", classOf[MasterCapsuleType])

  xstream.alias("SimpleTransitionType", SimpleTransitionType.getClass)
  xstream.alias("ExplorationTransitionType", ExplorationTransitionType.getClass)
  xstream.alias("AggregationTransitionType", AggregationTransitionType.getClass)
  xstream.alias("EndTransitionType", EndTransitionType.getClass)

  xstream.alias("Some", classOf[scala.Some[_]])
  xstream.alias("None", None.getClass)
  xstream.registerConverter(optionConverter)
  xstream.addImmutableType(None.getClass)

  implicit val mapper = xstream.getMapper

  xstream.alias("List", classOf[::[_]])
  xstream.alias("List", Nil.getClass)
  xstream.registerConverter(new ListConverter())
  xstream.addImmutableType(Nil.getClass)

  xstream.alias("HashMap", classOf[collection.immutable.HashMap[_, _]])
  xstream.alias("HashMap", collection.immutable.HashMap.empty.getClass.asInstanceOf[Class[_]])
  xstream.registerConverter(new HashMapConverter())

  def folder(clazz: Class[_]) =
    clazz match {
      case c if c == classOf[IPrototypeDataProxyUI] ⇒ "prototype"
      case c if c == classOf[IEnvironmentDataProxyUI] ⇒ "environment"
      case c if c == classOf[ISamplingCompositionDataProxyUI] ⇒ "sampling"
      case c if c == classOf[IHookDataProxyUI] ⇒ "hook"
      case c if c == classOf[ISourceDataProxyUI] ⇒ "source"
      case c if c == classOf[ITaskDataProxyUI] ⇒ "task"
      case c if c == classOf[CapsuleData] ⇒ "capsule"
      case c if c == classOf[TransitionData] ⇒ "transition"
      case c if c == classOf[SlotData] ⇒ "slot"
      case c if c == classOf[DataChannelData] ⇒ "datachannel"
      case c if c == classOf[MoleData] ⇒ "mole"
      case c ⇒ c.getSimpleName
    }

  def serializeConcept(clazz: Class[_], set: Iterable[(_, ID.Type)]) = {
    val conceptDir = new File(workDir, folder(clazz))
    conceptDir.mkdirs
    set.foreach {
      case (s, id) ⇒
        new File(conceptDir, id + ".xml").withWriter {
          xstream.toXML(s, _)
        }
    }
  }

  def serialize(file: File, proxies: Proxies, moleScenes: Iterable[MoleData]) = {
    serializeConcept(classOf[IPrototypeDataProxyUI], proxies.prototypes.map { s ⇒ s -> s.id })
    serializeConcept(classOf[IEnvironmentDataProxyUI], proxies.environments.map { s ⇒ s -> s.id })
    serializeConcept(classOf[ISamplingCompositionDataProxyUI], proxies.samplings.map { s ⇒ s -> s.id })
    serializeConcept(classOf[IHookDataProxyUI], proxies.hooks.map { s ⇒ s -> s.id })
    serializeConcept(classOf[ISourceDataProxyUI], proxies.sources.map { s ⇒ s -> s.id })
    serializeConcept(classOf[ITaskDataProxyUI], proxies.tasks.map { s ⇒ s -> s.id })

    serializeConcept(classOf[CapsuleData], moleScenes.flatMap(_.capsules).map { s ⇒ s -> s.id })
    serializeConcept(classOf[TransitionData], moleScenes.flatMap(_.transitions).map { s ⇒ s -> s.id })
    serializeConcept(classOf[DataChannelData], moleScenes.flatMap(_.dataChannels).map { s ⇒ s -> s.id })
    serializeConcept(classOf[SlotData], moleScenes.flatMap(_.slots).map { s ⇒ s -> s.id })

    serializeConcept(classOf[MoleData], moleScenes.map { ms ⇒ ms -> ms.id })

    val os = new TarOutputStream(new FileOutputStream(file))
    try os.createDirArchiveWithRelativePath(workDir)
    finally os.close
    clear
  }

  def read(f: File) = {
    try xstream.fromXML(f)
    catch {
      case e: Throwable ⇒
        throw new InternalProcessingError(e, "An error occurred when loading " + f.getAbsolutePath + "\n")
    }
  }

  def deserializeConcept[T](clazz: Class[_]) =
    new File(workDir, folder(clazz)).listFiles.toList.map(f ⇒ Try(read(f)).toOption).flatten.map(_.asInstanceOf[T])

  def deserialize(fromFile: String) = {
    val os = new TarInputStream(new FileInputStream(fromFile))
    os.extractDirArchiveWithRelativePathAndClose(workDir)

    val proxies: Proxies = new Proxies

    Try {
      deserializeConcept[IPrototypeDataProxyUI](classOf[IPrototypeDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[ISamplingCompositionDataProxyUI](classOf[ISamplingCompositionDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[IEnvironmentDataProxyUI](classOf[IEnvironmentDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[IHookDataProxyUI](classOf[IHookDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[ISourceDataProxyUI](classOf[ISourceDataProxyUI]).foreach(proxies.+=)
      deserializeConcept[ITaskDataProxyUI](classOf[ITaskDataProxyUI]).foreach(proxies.+=)

      deserializeConcept[CapsuleData](classOf[CapsuleData])
      deserializeConcept[SlotData](classOf[SlotData])
      deserializeConcept[TransitionData](classOf[TransitionData])
      deserializeConcept[DataChannelData](classOf[DataChannelData])

      val moleScenes = deserializeConcept[MoleData](classOf[MoleData])
      (proxies, moleScenes)
    }

  }

  def clear = {
    serializationStates.clear
    deserializationStates.clear
    workDir.recursiveDelete
    workDir.mkdirs
  }
}
