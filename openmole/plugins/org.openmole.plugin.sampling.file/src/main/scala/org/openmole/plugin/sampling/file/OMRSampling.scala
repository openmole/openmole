/*
 * Copyright (C) 2025 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.plugin.sampling.file

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import monocle.{Focus, Lens}

import org.openmole.core.format.*

object OMRSampling:

  given isSampling: IsSampling[OMRSampling] = s =>
    Sampling(
      s.apply(),
      s.outputs,
      s.inputs,
      s.validate
    )

  def apply(omrFile: File, variable: Seq[Val[?]]): OMRSampling =
    new OMRSampling(
      omr = OMRFormat.embedable(omrFile),
      variable = variable
    )

  def readVariables(omr: OMRFormat.V1.EmbedableContent, variable: Seq[Val[?]]): Seq[Variable[Array[?]]] =
    val ctx = Context(OMRFormat.variables(omr).head.variables *)
    variable.map: v =>
      ctx.variable[Array[?]](v.name) match
        case Some(v) => v
        case None =>
          ctx.bySimpleName[Array[?]](v.simpleName) match
            case s if s.isEmpty => throw new UserBadDataError(s"Variable $v not found in omr file ${omr.omrFile}")
            case s if s.size > 1 => throw new UserBadDataError(s"Variable $v not found in omr file ${omr.omrFile} using its fully qualified name and ambiguous when using its simple name: ${s.map(_.prototype).mkString(",")}")
            case s => s.head

case class OMRSampling(
  omr:           OMRFormat.V1.EmbedableContent,
  variable:      Seq[Val[?]]):

  def validate =
    Validate: v =>
      if !omr.omrFile.exists
      then Seq(UserBadDataError(s"File ${omr.omrFile} doesn't exist"))
      else
        val result =
          util.Try(OMRSampling.readVariables(omr, variable)).map: variableValues =>
            val line = variableValues.map(_.value).transpose.head
            (variable zip line).flatMap: (v, va) =>
              if !v.accepts(va)
              then Some(UserBadDataError(s"Value ${va} is not compatible with variable $v"))
              else None

        result match
          case util.Failure(f) => Seq(f)
          case util.Success(r) => r

  def inputs = Seq()
  def outputs = variable

  def apply() = FromContext: p =>
    import p.*
    val variableValues = OMRSampling.readVariables(omr, variable)

    def values =
      for line <- variableValues.map(_.value).transpose
      yield (variable zip line).map((v, va) => Variable.unsecureUntyped(v, va))

    values.toIterator



