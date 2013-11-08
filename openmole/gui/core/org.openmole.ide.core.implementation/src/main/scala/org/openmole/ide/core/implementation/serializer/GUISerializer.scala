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

import util.Try
import com.ice.tar.TarInputStream
import com.ice.tar.TarOutputStream
import com.thoughtworks.xstream.XStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.openmole.ide.misc.tools.util.ID
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.misc.tools.io.FileUtil._
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.io.TarArchiver._
import com.thoughtworks.xstream.io.{ HierarchicalStreamReader, HierarchicalStreamWriter }
import org.openmole.misc.exception.{ UserBadDataError, InternalProcessingError }
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.{ ErrorWriter, UnmarshallingContext, MarshallingContext }
import collection.mutable
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import org.openmole.misc.tools.service.Logger
import org.openmole.ide.core.implementation.commons._
import scala.util.Failure
import scala.Some
import org.openmole.ide.core.implementation.commons.MasterCapsuleType
import scala.util.Success
import org.openmole.ide.core.implementation.sampling.DomainProxyUI
import java.net.URL
import scalaz._
import Scalaz._
import org.openmole.core.serializer.file._

object GUISerializer extends Logger {
  var instance = new GUISerializer

  implicit def urlToFile(url: URL): File = new File(url.toURI)

  def unserialise(url: URL) = instance.read(url)

  def serializable(url: URL): Boolean = try {
    unserialise(url)
    true
  }
  catch {
    case x: Throwable ⇒ false
  }
}

class GUISerializer extends FileListing with FileInjection { serializer ⇒

  sealed trait SerializationState {
    def id: ID.Type
  }

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
          serializeConcept(clazz.runtimeClass, List(dataUI -> dataUI.id))
          marshal(dataUI, writer, mc)
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
        val id = reader.getAttribute("id")
        val dui = existing(id)
        dui match {
          case Some(y) ⇒ y
          case _       ⇒ serializer.deserializeConcept(uc.getRequiredType, id)
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

    override def canConvert(t: Class[_]) =
      clazz.runtimeClass.isAssignableFrom(t)

    def existing(id: String) = deserializationStates.get(id)
    def add(e: T) = deserializationStates.put(e.id, e)

  }

  val taskConverter = new GUIConverter[TaskDataProxyUI]
  val prototypeConverter = new GUIConverter[PrototypeDataProxyUI]
  val samplingConverter = new GUIConverter[SamplingCompositionDataProxyUI]
  val domainConverter = new GUIConverter[DomainProxyUI]
  val environmentConverter = new GUIConverter[EnvironmentDataProxyUI]
  val hookConverter = new GUIConverter[HookDataProxyUI]
  val sourceConverter = new GUIConverter[SourceDataProxyUI]
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
          GUISerializer.logger.log(GUISerializer.WARNING, "Error in deserialisation", t)
          for (i ← 0 until cReader.depth) reader.moveUp
          None
      }
    }

    override def canConvert(t: Class[_]) = classOf[Some[_]].isAssignableFrom(t)
  }

  val updateConverter = new ReflectionConverter(xstream.getMapper, xstream.getReflectionProvider) {
    override def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) =
      updated(super.unmarshal(reader, context))

    override def marshal(
      o: Object,
      writer: HierarchicalStreamWriter,
      mc: MarshallingContext) =
      super.marshal(updated(o), writer, mc)

    override def canConvert(t: Class[_]) = classOf[Update[_]].isAssignableFrom(t)

    def updated(u: AnyRef): AnyRef =
      if (classOf[Update[_]].isAssignableFrom(u.getClass))
        updated(u.asInstanceOf[Update[AnyRef]].update)
      else u
  }

  xstream.registerConverter(taskConverter)
  xstream.registerConverter(prototypeConverter)
  xstream.registerConverter(samplingConverter)
  xstream.registerConverter(domainConverter)
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
  xstream.addImmutableType(classOf[MoleData2])

  xstream.alias("DataChannelData", classOf[DataChannelData])
  xstream.alias("MoleData", classOf[MoleData])
  xstream.alias("MoleData2", classOf[MoleData2])
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

  xstream.registerConverter(updateConverter)

  implicit val mapper = xstream.getMapper

  xstream.alias("List", classOf[::[_]])
  xstream.alias("List", Nil.getClass)
  xstream.registerConverter(new ListConverter())
  xstream.addImmutableType(Nil.getClass)

  xstream.alias("HashMap", classOf[collection.immutable.HashMap[_, _]])
  xstream.alias("HashMap", collection.immutable.HashMap.empty.getClass.asInstanceOf[Class[_]])
  xstream.registerConverter(new HashMapConverter())

  implicit class ClassDecorator(c: Class[_]) {
    def >(o: Class[_]) = c.isAssignableFrom(o)
    def <(o: Class[_]) = o.isAssignableFrom(c)
  }

  def folder(clazz: Class[_]) =
    clazz match {
      case c if c < classOf[PrototypeDataProxyUI] ⇒ "prototype"
      case c if c < classOf[EnvironmentDataProxyUI] ⇒ "environment"
      case c if c < classOf[SamplingCompositionDataProxyUI] ⇒ "sampling"
      case c if c < classOf[DomainProxyUI] ⇒ "domain"
      case c if c < classOf[HookDataProxyUI] ⇒ "hook"
      case c if c < classOf[SourceDataProxyUI] ⇒ "source"
      case c if c < classOf[TaskDataProxyUI] ⇒ "task"
      case c if c < classOf[CapsuleData] ⇒ "capsule"
      case c if c < classOf[TransitionData] ⇒ "transition"
      case c if c < classOf[SlotData] ⇒ "slot"
      case c if c < classOf[DataChannelData] ⇒ "datachannel"
      case c if c < classOf[MoleData] || c < classOf[MoleData2] ⇒ "mole"
      case c ⇒ c.getSimpleName
    }

  def serializeConcept(clazz: Class[_], set: Iterable[(_, ID.Type)]) = {
    val conceptDir = new File(workDir, folder(clazz))
    conceptDir.mkdirs
    set.foreach {
      case (s, id) ⇒
        val conceptFile = conceptDir.child(id + ".xml")
        if (!conceptFile.exists) {
          conceptFile.withWriter {
            xstream.toXML(s, _)
          }
        }
    }
  }

  def serializeMetadata(metaData: Option[MetaData]) =
    for {
      md ← metaData
    } {
      val imagePath = workDir.child("/metadata/img")
      imagePath.mkdirs
      md.scenes.foreach {
        s ⇒
          s.buildImage(imagePath.child(s.dataUI.id + ".png"))
      }
    }

  def serialize(file: File, proxies: Proxies, moleScenes: Iterable[MoleData2], metaData: Option[MetaData] = None) = {
    serializeConcept(classOf[PrototypeDataProxyUI], proxies.prototypes.map { s ⇒ s -> s.id })
    serializeConcept(classOf[EnvironmentDataProxyUI], proxies.environments.map { s ⇒ s -> s.id })
    serializeConcept(classOf[SamplingCompositionDataProxyUI], proxies.samplings.map { s ⇒ s -> s.id })
    serializeConcept(classOf[HookDataProxyUI], proxies.hooks.map { s ⇒ s -> s.id })
    serializeConcept(classOf[SourceDataProxyUI], proxies.sources.map { s ⇒ s -> s.id })
    serializeConcept(classOf[TaskDataProxyUI], proxies.tasks.map { s ⇒ s -> s.id })

    serializeConcept(classOf[CapsuleData], moleScenes.flatMap(_.capsules).map { s ⇒ s -> s.id })
    serializeConcept(classOf[TransitionData], moleScenes.flatMap(_.transitions).map { s ⇒ s -> s.id })
    serializeConcept(classOf[DataChannelData], moleScenes.flatMap(_.dataChannels).map { s ⇒ s -> s.id })
    serializeConcept(classOf[SlotData], moleScenes.flatMap(_.slots).map { s ⇒ s -> s.id })

    serializeConcept(classOf[MoleData2], moleScenes.map { ms ⇒ ms -> ms.id })

    serializeMetadata(metaData)

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

  def deserializeConcept[T](clazz: Class[_]): Writer[List[Throwable], List[T]] = {
    val res = new File(workDir, folder(clazz)).listFiles.toList.map(
      f ⇒ Try(read(f).asInstanceOf[T])
    )
    res.collect { case Success(s) ⇒ s }.set(res.collect { case Failure(f) ⇒ f })
  }

  def deserializeConcept(clazz: Class[_], id: String) = {
    val f = workDir.child(folder(clazz)).child(id + ".xml")
    read(f)
  }

  def deserialize(fromFile: String) = {
    val os = new TarInputStream(new FileInputStream(fromFile))
    try os.extractDirArchiveWithRelativePath(workDir)
    finally os.close

    val concepts =
      for {
        protos ← deserializeConcept[PrototypeDataProxyUI](classOf[PrototypeDataProxyUI])
        samplings ← deserializeConcept[SamplingCompositionDataProxyUI](classOf[SamplingCompositionDataProxyUI])
        envs ← deserializeConcept[EnvironmentDataProxyUI](classOf[EnvironmentDataProxyUI])
        hooks ← deserializeConcept[HookDataProxyUI](classOf[HookDataProxyUI])
        sources ← deserializeConcept[SourceDataProxyUI](classOf[SourceDataProxyUI])
        tasks ← deserializeConcept[TaskDataProxyUI](classOf[TaskDataProxyUI])
      } yield protos ++ samplings ++ envs ++ hooks ++ sources ++ tasks

    /*deserializeConcept[CapsuleData](classOf[CapsuleData])
      deserializeConcept[SlotData](classOf[SlotData])
      deserializeConcept[TransitionData](classOf[TransitionData])
      deserializeConcept[DataChannelData](classOf[DataChannelData])*/

    for {
      concept ← concepts
      moleScenes ← deserializeConcept[MoleData2](classOf[MoleData2])
    } yield {
      val proxies: Proxies = new Proxies
      concept.foreach(proxies += _)
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
