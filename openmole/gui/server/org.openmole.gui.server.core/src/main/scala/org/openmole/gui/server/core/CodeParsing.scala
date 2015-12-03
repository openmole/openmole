package org.openmole.gui.server.core

import org.openmole.gui.ext.data._
import org.clapper.classutil.ClassFinder
import scala.io.Source
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.reflect.runtime.{ universe ⇒ ru }
import org.openmole.gui.server.core.Utils._

/*
 * Copyright (C) 10/11/15 // mathieu.leclaire@openmole.org
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

object CodeParsing {

  def fromCommand(command: Seq[String]) = {
    val (language, codeName, commandElements: Seq[CommandElement]) = command.headOption match {
      case Some("python") ⇒ (Some(PythonLanguage()), command.lift(1).getOrElse(""), indexArgs(command.drop(2), Seq()).map { case (k, v, i) ⇒ toVariableElement(k, v, i, CareTaskType()) })
      case Some("R")      ⇒ (Some(RLanguage()), "", rParsing(command.drop(1), CareTaskType()))
      case _              ⇒ (None, command.head, command.drop(1))
    }

    //Parse the arguments and return the LaunchingCommand

    Some(
      LaunchingCommand(
        language,
        codeName,
        commandElements)
    )
  }

  def fromFile(safePath: SafePath) = {
    safePath.name.split(".").last match {
      case "nlogo" ⇒ Some(netlogoParsing(safePath))
      case _       ⇒ None
    }
  }

  private def isFileString(fs: Option[String]) = fs match {
    case Some(s: String) ⇒ s matches ("""((.*)[.]([^.]+))|(.*/.*)""")
    case _               ⇒ false
  }

  private def indexArgs(args: Seq[String], indexed: Seq[(Option[String], Option[String], Int)]): Seq[(Option[String], Option[String], Int)] = {
    if (args.isEmpty) indexed
    else {
      val head = args.head
      val nextIndex = indexed.lastOption.map {
        _._3
      }.getOrElse(-1) + 1
      if (head.startsWith("-")) {
        indexArgs(args.drop(2), indexed :+ (
          Some(head), {
            if (args.size >= 2) {
              if (!args(1).startsWith("-")) Some(args(1)) else None
            }
            else None
          }, nextIndex)
        )
      }
      else indexArgs(args.drop(1), indexed :+ (None, Some(head), nextIndex))
    }
  }

  private def toVariableElement(key: Option[String], value: Option[String], index: Int, taskType: TaskType): VariableElement = {
    val isFile = isFileString(value)
    VariableElement(index, ProtoTypePair(key match {
      case Some(k: String) ⇒ k
      case _ ⇒ if (isFile) {
        value match {
          case Some(v: String) ⇒ v.split('/').last.split('.').head
          case _               ⇒ "i" + index
        }
      }
      else "i" + index
    },
      value match {
        case Some(a: String) ⇒ if (isFile) ProtoTYPE.FILE else ProtoTYPE.DOUBLE
        case _               ⇒ ProtoTYPE.DOUBLE
      },
      if (isFile) "" else value.getOrElse(""),
      if (isFile) value else None
    ),
      taskType
    )
  }

  private def rParsing(args: Seq[String], taskType: TaskType): Seq[CommandElement] = {
    val indexed = indexArgs(args, Seq())

    val (others, toBeParsed) = indexed.partition { p ⇒
      Seq("--slave", "--args").contains(p._1.getOrElse(""))
    }

    val (f, rest) = toBeParsed.partition { p ⇒ p._1 == Some("-f") }

    rest.map { r ⇒
      val (k, v, i) = r
      toVariableElement(k, v, i, taskType)
    } ++ others.map { case (k, _, i) ⇒ k.map { kk ⇒ StaticElement(i, kk) } }.flatten ++ f.map { x ⇒ StaticElement(x._3, "-f " + x._2.getOrElse("")) }
  }

  def netlogoParsing(safePath: SafePath): LaunchingCommand = {

    val lines = Source.fromFile(safePath).getLines.toArray

    def parse(lines: Seq[(String, Int)], args: Seq[ProtoTypePair], outputs: Seq[ProtoTypePair]): (Seq[ProtoTypePair], Seq[ProtoTypePair]) = {
      if (lines.isEmpty) (args, outputs)
      else {
        val (line, index) = lines.head
        val tail = lines.tail
        if (line.startsWith("SLIDER")) parse(tail, args :+ parseSlider(index), outputs)
        else if (line.startsWith("SWITCH")) parse(tail, args :+ parseSwitch(index), outputs)
        else if (line.startsWith("INPUTBOX")) parse(tail, args :+ parseInputBox(index), outputs)
        else if (line.startsWith("CHOOSER")) parse(tail, args :+ parseChooser(index), outputs)
        else if (line.startsWith("MONITOR")) parse(tail, args, outputs ++ parseMonitor(index))
        else if (line.startsWith("PLOT")) parse(tail, args, outputs ++ parsePlot(index))
        else parse(tail, args, outputs)
      }
    }

    implicit class CleanName(s: String) {
      def clean = s.split('-').reduce(_ + _.capitalize).filterNot(Seq('?').contains)
    }

    def parseSlider(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 9), Some(name))
    }

    def parseSwitch(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.BOOLEAN, lines(start + 7), Some(name))
    }

    def parseInputBox(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.DOUBLE, lines(start + 6), Some(name))
    }

    def parseMonitor(start: Int): Seq[ProtoTypePair] = {
      val name = lines(start + 6).clean.split(' ')
      if (name.size == 1) Seq(ProtoTypePair(name.head.clean, ProtoTYPE.DOUBLE, mapping = Some(name.head)))
      else Seq()
    }

    def parsePlot(start: Int): Seq[ProtoTypePair] = (lines(start + 6).split(',') ++ lines(start + 7).split(',')).map {
      _.replaceAll(" ", "")
    }.filterNot { v ⇒
      Seq("ticks", "time").contains(v)
    }.map {
      n ⇒ ProtoTypePair(n.clean, ProtoTYPE.DOUBLE, mapping = Some(n))
    }

    def parseChooser(start: Int): ProtoTypePair = {
      val name = lines(start + 5)
      ProtoTypePair(name.clean, ProtoTYPE.STRING, lines(start + 7).split(' ').head, Some(name))
    }

    val (args, outputs) = parse(lines.toSeq.zipWithIndex, Seq(), Seq())

    LaunchingCommand(
      Some(NetLogoLanguage()), "",
      args.distinct.zipWithIndex.map { case (a, i) ⇒ VariableElement(i, a, NetLogoTaskType()) },
      outputs.distinct.zipWithIndex.map { case (o, i) ⇒ VariableElement(i, o, NetLogoTaskType()) }
    )
  }

  def jarParsing(safePath: SafePath): LaunchingCommand = {
    println("JAR PARSING")
    val classLoader = new URLClassLoader(Seq(safePath.toURI.toURL), this.getClass.getClassLoader)
    val mirror = ru.runtimeMirror(classLoader)
    // val urls = classLoader.getURLs

    //println("URLs " + urls)

    val classes = ClassFinder(Seq(safePath)).getClasses.toSeq.filterNot {
      c ⇒
        Seq("scala", "java").exists {
          ex ⇒ c.name.startsWith(ex)
        }
    }

    println("Methods " + classes.size + " // " + classes.map { c ⇒
      c.name + " // " + c.methods.map { m ⇒
        println("METH " + m.toString())
        println("Modifiers: " + m.modifiers.map {
          _.toString
        })
      }
    })

    classes.map { c ⇒
      println("NAME " + c.name)
      val oo = mirror.staticClass(c.name).typeSignature.baseClasses.map {
        bc ⇒ bc.fullName

      }
      println("OO " + oo)
    }

    LaunchingCommand(None, "")

  }

}
