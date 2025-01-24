/*
 * Copyright (C) 2010 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.task

import org.openmole.core.context.{ Context, Val, Variable }
import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole.{ Mole, MoleCapsule, Sources, Hooks }
import org.openmole.core.workflow.sampling._

import scala.collection.immutable.TreeMap
import scala.collection.mutable.ArrayBuffer
import cats.implicits._
import org.openmole.core.setter._

import scala.collection.mutable

/**
 * ExplorationTask is a wrapper for a sampling
 */
object ExplorationTask:

  /**
   * Explore a given sampling: gets the prototype values in the sampling from the context to construct the context with all values,
   * wrapped as a [[FromContextTask]].
   * Values assignment is done with insecure casting, and an exception is caught and piped as a [[UserBadDataError]] if the conversion can not be done.
   *
   * @param sampling
   * @return
   */
  inline def apply[S](s: S)(implicit name: sourcecode.Name, definitionScope: DefinitionScope, inline isSampling: IsSampling[S]): FromContextTask =
    def sampling() = isSampling(s)
    build(sampling)

  inline def build(inline sampling: () => Sampling)(using sourcecode.Name, DefinitionScope) =
    FromContextTask("ExplorationTask") { p =>
      import p.*

      val variablesValues =
        val sValue = sampling()
        val samplingValue = sValue.sampling.from(context).toVector

        val values =
          TreeMap.empty[Val[?], Array[?]] ++ sValue.outputs.map { p =>
             p -> p.`type`.manifest.newArray(samplingValue.size)
          }

        for
           (sample, i) <- samplingValue.zipWithIndex
           v <- sample
        do
          values.get(v.prototype) match
            case Some(b) => java.lang.reflect.Array.set(b, i, v.value)
            case None    => throw new InternalProcessingError(s"Missing sample value for variable $v at position $i")

        values

       variablesValues.map { (k, v) =>
         try Variable.unsecure(k.toArray, v)
         catch
           case e: ArrayStoreException => throw new UserBadDataError("Cannot fill factor values in " + k.toArray + ", values " + v)
       }: Context
     } set (
       inputs ++= sampling().inputs.toSeq,
       exploredOutputs ++= sampling().outputs.toSeq.map(_.toArray)
     ) withValidate { sampling().validate }



  /**
   * Given a [[MoleCapsule]], function to test if a given prototype is explored by it
   * @param c
   * @return
   */
  def explored(c: MoleCapsule, mole: Mole, sources: Sources, hooks: Hooks) =
    val taskOutputs = Task.outputs(c.task(mole, sources, hooks))
    (p: Val[?]) => taskOutputs.explored(p)
