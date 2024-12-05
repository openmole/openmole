/*
 * Copyright (C) 2015 Romain Reuillon
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

package org.openmole.core

import cats.*

import org.openmole.core.workspace.TmpDirectory
import org.openmole.core.logconfig.LoggerConfig
import org.openmole.core.workflow.mole
import org.openmole.core.serializer.SerializerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.fileservice.FileService
import org.openmole.tool.crypto.Cypher
import org.openmole.core.context.Context
import squants.information.*

import scala.collection.immutable.NumericRange

package dsl:
  trait DSLPackage:
    export org.openmole.core.context.Val
    export org.openmole.core.workflow.execution.LocalEnvironment

    export org.openmole.core.workflow.task.EmptyTask
    export org.openmole.core.workflow.task.ExplorationTask
    export org.openmole.core.workflow.task.MoleTask
    export org.openmole.core.workflow.task.TryTask
    export org.openmole.core.workflow.task.RetryTask

    export org.openmole.core.workflow.hook.display
    export org.openmole.core.format.{OMROutputFormat, OMROption}

    def bundles = PluginManager.bundleFiles
    def dependencies(file: File) = PluginManager.dependencies(file)

    object omr:
      def toCSV(file: File, destination: File)(using SerializerService) = org.openmole.core.format.OMRFormat.writeCSV(file, destination)
      def toJSON(file: File, destination: File)(using SerializerService) = org.openmole.core.format.OMRFormat.writeJSON(file, destination)
      def copyFiles(file: File, destination: File) = org.openmole.core.format.OMRFormat.resultFileDirectory(file).foreach(_.copy(destination))
      def variables(file: File)(using SerializerService) = org.openmole.core.format.OMRFormat.variables(file)

    def load(file: File)(implicit serialiserService: SerializerService) = serialiserService.deserialize[Object](file)
    def loadArchive(file: File)(implicit newFile: TmpDirectory, serialiserService: SerializerService, fileService: FileService) = serialiserService.deserializeAndExtractFiles[Object](file, deleteFilesOnGC = true, gz = true)
    def load(file: String)(implicit serialiserService: SerializerService): Object = load(new File(file))
    def loadArchive(file: String)(implicit newFile: TmpDirectory, serialiserService: SerializerService, fileService: FileService): Object = loadArchive(new File(file))
    def save(obj: Object, file: File)(implicit serialiserService: SerializerService) = serialiserService.serialize(obj, file)
    def saveArchive(obj: Object, file: File)(implicit newFile: TmpDirectory, serialiserService: SerializerService) = serialiserService.serializeAndArchiveFiles(obj, file, gz = true)
    def save(obj: Object, file: String)(implicit serialiserService: SerializerService): Unit = save(obj, new File(file))
    def saveArchive(obj: Object, file: String)(implicit newFile: TmpDirectory, serialiserService: SerializerService): Unit = saveArchive(obj, new File(file))


    implicit lazy val implicitContext: Context = Context.empty

    //implicit lazy val workspace = Workspace
    lazy val logger = LoggerConfig

    //implicit def decrypt = Decrypt(workspace)

    implicit def stringToFile(path: String): File = File(path)

    export squants.time.TimeConversions.TimeConversions
    //implicit def timeConversion[N: Numeric](n: N): squants.Time.TimeConversions = squants.time.TimeConversions.TimeConversions(n)
    extension [N: Numeric](n: N)
      def nanosecond = n nanoseconds
      def microsecond = n microseconds
      def millisecond = n milliseconds
      def second = n seconds
      def minute = n minutes
      def hour = n hours
      def day = n days

    export squants.information.InformationConversions.InformationConversions

    //implicit def informationUnitConversion[N: Numeric](n: N): squants.information.InformationConversions.InformationConversions = squants.information.InformationConversions.InformationConversions(n)
    extension [N: Numeric](n: N)
      def byte = n bytes
      def kilobyte = n kilobytes
      def megabyte = n megabytes
      def gigabyte = n gigabytes
      def terabyte = n terabytes
      def petabyte = n petabytes
      def exabyte = n exabytes
      def zettabyte = n zettabytes
      def yottabyte = n yottabytes

    //implicit def intToMemory(i: Int): Information = (i megabytes)
    //implicit def intToMemoryOptional(i: Int): OptionalArgument[Information] = OptionalArgument(intToMemory(i))

    def encrypt(s: String)(implicit cypher: Cypher) = cypher.encrypt(s)

    implicit def seqIsFunctor: Functor[Seq] = new Functor[Seq]:
      override def map[A, B](fa: Seq[A])(f: (A) ⇒ B): Seq[B] = fa.map(f)

    type Data = File

    //export org.openmole.tool.collection.DoubleRangeDecorator
    @inline implicit class DoubleWrapper(d: Double):
      infix def to(h: Double) = org.openmole.tool.collection.DoubleRange.to(d, h)
      infix def until(h: Double) = org.openmole.tool.collection.DoubleRange.until(d, h)

    //implicit def doubleRange(d: Double): org.openmole.tool.collection.DoubleRangeDecorator = new org.openmole.tool.collection.DoubleRangeDecorator(d)
    export Predef.longWrapper
    export Predef.intWrapper
    export Predef.doubleWrapper


package object dsl extends dsl.DSLPackage
  with workflow.ExportedPackage
  with cats.instances.AllInstances