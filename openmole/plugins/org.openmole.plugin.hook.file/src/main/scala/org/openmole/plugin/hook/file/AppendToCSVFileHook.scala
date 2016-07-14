/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.hook.file

import org.openmole.core.tools.io.Prettifier
import org.openmole.tool.stream._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools._
import org.openmole.core.workflow.validation._
import Prettifier._
import monocle.macros.Lenses

import scala.annotation.tailrec
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }

object AppendToCSVFileHook {

  implicit def isIO: InputOutputBuilder[AppendToCSVFileHook] = InputOutputBuilder(AppendToCSVFileHook.config)

  implicit def isBuilder: AppendToCSVFileHookBuilder[AppendToCSVFileHook] = new AppendToCSVFileHookBuilder[AppendToCSVFileHook] {
    override def csvHeader = AppendToCSVFileHook.header
    override def arraysOnSingleRow = AppendToCSVFileHook.arraysOnSingleRow
  }

  def apply(fileName: FromContext[String], prototypes: Prototype[_]*) =
    new AppendToCSVFileHook(
      fileName,
      prototypes.toVector,
      header = None,
      arraysOnSingleRow = false,
      config = InputOutputConfig()
    ) set (inputs += (prototypes: _*))

}

@Lenses case class AppendToCSVFileHook(
    fileName:          FromContext[String],
    prototypes:        Vector[Prototype[_]],
    header:            Option[FromContext[String]],
    arraysOnSingleRow: Boolean,
    config:            InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Prototype[_]]): Seq[Throwable] =
    fileName.validate(inputs) ++ header.toSeq.flatMap(_.validate(inputs))

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val file = new File(fileName.from(context))
    file.createParentDir

    val ps =
      if (prototypes.isEmpty) context.values.map { _.prototype }
      else prototypes

    file.withLock {
      fos ⇒
        if (file.size == 0)
          fos.appendLine {
            def defaultHeader = ps.map { _.name }.mkString(",")
            header.map(_.from(context)) getOrElse defaultHeader
          }

        val lists = ps.map {
          p ⇒
            context.option(p) match {
              case Some(v) ⇒
                v match {
                  case v: Array[_] ⇒ v.toList
                  case v           ⇒ List(v)
                }
              case None ⇒ List("not found")
            }
        }.toList

        def moreThanOneElement(l: List[_]) = !l.isEmpty && !l.tail.isEmpty

        def flatAny(o: Any): List[Any] = o match {
          case o: List[_] ⇒ o
          case _          ⇒ List(o)
        }

        @tailrec def write(lists: List[List[_]]): Unit =
          if (arraysOnSingleRow || !lists.exists(moreThanOneElement))
            writeLine(lists.flatten(flatAny))
          else {
            writeLine(lists.map { _.head })
            write(lists.map { l ⇒ if (moreThanOneElement(l)) l.tail else l })
          }

        def writeLine[T](list: List[T]) = {
          fos.appendLine(list.map(l ⇒ {
            val prettified = l.prettify()
            def shouldBeQuoted = prettified.contains(',') || prettified.contains('"')
            def quote(s: String) = '"' + s.replaceAll("\"", "\"\"") + '"'
            if (shouldBeQuoted) quote(prettified) else prettified
          }).mkString(","))
        }

        write(lists)
    }
    context
  }

}
