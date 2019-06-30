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

package dsl {

  import org.openmole.core.context._
  import org.openmole.core.logconfig.LoggerConfig
  import cats._
  import org.openmole.core.workflow.mole
  import org.openmole.tool.crypto.Cypher
  import squants.information._

  trait DSLPackage <: Commands
    with Serializer
    with Classes
    with workflow.ExportedPackage
    with cats.instances.AllInstances {

    implicit lazy val implicitContext = Context.empty

    //implicit lazy val workspace = Workspace
    lazy val logger = LoggerConfig

    //implicit def decrypt = Decrypt(workspace)

    implicit def stringToFile(path: String) = File(path)

    implicit def timeConversion[N: Numeric](n: N) = squants.time.TimeConversions.TimeConversions(n)
    implicit class singularTimeConversion[N: Numeric](n: N) {
      def nanosecond = n nanoseconds
      def microsecond = n microseconds
      def millisecond = n milliseconds
      def second = n seconds
      def minute = n minutes
      def hour = n hours
      def day = n days
    }

    implicit def informationUnitConversion[N: Numeric](n: N) = squants.information.InformationConversions.InformationConversions(n)
    implicit class singularInformationUnitConversion[N: Numeric](n: N) {
      def byte = n bytes
      def kilobyte = n kilobytes
      def megabyte = n megabytes
      def gigabyte = n gigabytes
      def terabyte = n terabytes
      def petabyte = n petabytes
      def exabyte = n exabytes
      def zettabyte = n zettabytes
      def yottabyte = n yottabytes
    }

    implicit def intToMemory(i: Int): Information = (i megabytes)
    implicit def intToMemoryOptional(i: Int): OptionalArgument[Information] = OptionalArgument(intToMemory(i))

    def encrypt(s: String)(implicit cypher: Cypher) = cypher.encrypt(s)

    implicit def seqIsFunctor = new Functor[Seq] {
      override def map[A, B](fa: Seq[A])(f: (A) ⇒ B): Seq[B] = fa.map(f)
    }

    type Data = File
  }

  object extension {
    type FromContext[T] = org.openmole.core.expansion.FromContext[T]
    lazy val FromContext = org.openmole.core.expansion.FromContext

    type DefinitionScope = org.openmole.core.workflow.builder.DefinitionScope
    def DefinitionScope = org.openmole.core.workflow.builder.DefinitionScope

    type CacheKey[T] = org.openmole.tool.cache.CacheKey[T]
    def CacheKey = org.openmole.tool.cache.CacheKey

    type ScalarOrSequenceOfDouble[T] = org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble[T]
    def ScalarOrSequenceOfDouble = org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble

    type Hook = mole.FromContextHook
    def Hook = org.openmole.core.workflow.mole.Hook

    type Source = org.openmole.core.workflow.mole.Source
    type FromContextSource = org.openmole.core.workflow.mole.FromContextSource
    def Source = org.openmole.core.workflow.mole.Source

    type Task = org.openmole.core.workflow.task.Task
    type FromContextTask = org.openmole.core.workflow.task.FromContextTask
    def Task = org.openmole.core.workflow.task.FromContextTask

    type Sampling = org.openmole.core.workflow.sampling.Sampling
    type FromContextSampling = org.openmole.core.workflow.sampling.FromContextSampling
    def Sampling = org.openmole.core.workflow.sampling.Sampling

    type Namespace = org.openmole.core.context.Namespace
    def Namespace = org.openmole.core.context.Namespace

    type Variable[T] = org.openmole.core.context.Variable[T]
    def Variable = org.openmole.core.context.Variable

    type Context = org.openmole.core.context.Context
    def Context = org.openmole.core.context.Context

    type WritableOutput = org.openmole.core.workflow.tools.WritableOutput
    def WritableOutput = org.openmole.core.workflow.tools.WritableOutput

    def ExpandedString = org.openmole.core.expansion.ExpandedString

    implicit def validationOfFromContext(f: FromContext[_]) =
      (p: FromContext.ValidationParameters) ⇒ f.validate(p.inputs)(p.newFile, p.fileService)

    type Negative[A] = org.openmole.core.keyword.Negative[A]
    type Under[A, B] = org.openmole.core.keyword.Under[A, B]
    type In[A, B] = org.openmole.core.keyword.In[A, B]
    type :=[A, B] = org.openmole.core.keyword.:=[A, B]
    type Aggregate[A, B] = org.openmole.core.keyword.Aggregate[A, B]

    def Aggregate = org.openmole.core.keyword.Aggregate
  }

}

package object dsl extends DSLPackage
