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

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.expansion._
import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.dsl._
import org.openmole.tool.stream._

import scala.annotation.tailrec

object AppendToCSVFileHook {

  implicit def isIO: InputOutputBuilder[AppendToCSVFileHook] = InputOutputBuilder(AppendToCSVFileHook.config)

  implicit def isBuilder: AppendToCSVFileHookBuilder[AppendToCSVFileHook] = new AppendToCSVFileHookBuilder[AppendToCSVFileHook] {
    override def csvHeader = AppendToCSVFileHook.header
    override def arraysOnSingleRow = AppendToCSVFileHook.arraysOnSingleRow
  }

  def apply(file: FromContext[File], prototypes: Val[_]*)(implicit name: sourcecode.Name) =
    new AppendToCSVFileHook(
      file,
      prototypes.toVector,
      header = None,
      arraysOnSingleRow = false,
      config = InputOutputConfig()
    ) set (inputs += (prototypes: _*))

}

@Lenses case class AppendToCSVFileHook(
    file:              FromContext[File],
    prototypes:        Vector[Val[_]],
    header:            Option[FromContext[String]],
    arraysOnSingleRow: Boolean,
    config:            InputOutputConfig
) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    file.validate(inputs) ++ header.toSeq.flatMap(_.validate(inputs))
  }

  override protected def process(executionContext: MoleExecutionContext) = FromContext { parameters ⇒
    import parameters._
    val f = file.from(context)
    f.createParentDir

    val ps =
      if (prototypes.isEmpty) context.values.map { _.prototype }
      else prototypes

    f.withLock {
      fos ⇒

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

        if (f.size == 0)
          fos.appendLine {
            def defaultHeader =
              (ps zip lists).flatMap {
                case (p, l) ⇒
                  if (arraysOnSingleRow && moreThanOneElement(l))
                    (0 until l.size).map(i ⇒ s"${p.name}$i")
                  else List(p.name)
              }.mkString(",")

            header.map(_.from(context)) getOrElse defaultHeader
          }

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
