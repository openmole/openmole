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
package org.openmole.core.workflow.tools

import java.io.File
import java.lang.reflect.Method

import org.openmole.core.console._
import org.openmole.core.exception._
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.tools.obj.ClassUtils._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.task.Task
import org.openmole.core.workflow.validation.TypeUtil
import org.osgi.framework.Bundle

import scala.util.{ Random, Try }

trait ScalaCompilation {

  def plugins: Seq[File]
  def libraries: Seq[File]

  def compile(code: String) = Try {
    val interpreter = new ScalaREPL(plugins.flatMap(PluginManager.bundle) ++ Seq(PluginManager.bundleForClass(this.getClass)), libraries)

    val evaluated = interpreter.eval(code)

    if (evaluated == null) throw new InternalProcessingError(
      s"""The return value of the script was null:
         |$code""".stripMargin
    )

    evaluated
  }

}

object ScalaWrappedCompilation {
  def inputObject = "input"

  def static[R](code: String, _inputs: Seq[Prototype[_]], output: CompilationOutput[R] = ScalaRawOutput[R]()) = {
    val compilation =
      new ScalaWrappedCompilation with StaticHeader {
        type RETURN = R

        override def inputs = _inputs
        override val compilationOutput = output
        override def source: String = code
        override def imports: Seq[String] = Seq.empty
        override def plugins: Seq[File] = Seq.empty
        override def libraries: Seq[File] = Seq.empty
      }

    compilation.functionCode.get
    compilation
  }


  def dynamic[R](code: String, output: CompilationOutput[R] = ScalaRawOutput[R]()) =
    new ScalaWrappedCompilation with DynamicHeader {
      type RETURN = R

      override val compilationOutput = output
      override def source: String = code
      override def imports: Seq[String] = Seq.empty
      override def plugins: Seq[File] = Seq.empty
      override def libraries: Seq[File] = Seq.empty
    }

  type ScalaClosure = (Context, RandomProvider) ⇒ Any

  trait CompiledScala[RETURN] {
    def run(context: Context)(implicit rng: RandomProvider): RETURN
  }

}

import ScalaWrappedCompilation._


trait CompilationOutput[RETURN] {
  def apply(closure: ScalaClosure): CompiledScala[RETURN]
  def wrapOutput: String
}

object CompilationOutput {

  case class WrappedScala(outputs: PrototypeSet, compiled: (Context, RandomProvider) ⇒ java.util.Map[String, Any]) extends CompiledScala[Context] {
    def run(context: Context)(implicit rng: RandomProvider): Context = {
      val map = compiled(context, rng)
      context ++
        outputs.toSeq.map {
          o ⇒ Variable.unsecure(o, Option(map.get(o.name)).getOrElse(new InternalProcessingError(s"Not found output $o")))
        }
    }

    def wrapOutput = ""
  }

  case class RawScala[T](compiled: ScalaClosure) extends CompiledScala[T] {
    def run(context: Context)(implicit rng: RandomProvider): T = compiled(context, rng).asInstanceOf[T]
  }

}

case class ScalaWrappedOutput(outputs: PrototypeSet) extends CompilationOutput[Context] { compilation ⇒

  def apply(closure: ScalaClosure) =
    CompilationOutput.WrappedScala(
      compiled = closure.asInstanceOf[(Context, RandomProvider) ⇒ java.util.Map[String, Any]],
      outputs = compilation.outputs
    )

  def wrapOutput =
    s"""
       |import scala.collection.JavaConversions.mapAsJavaMap
       |mapAsJavaMap(Map[String, Any]( ${outputs.toSeq.map(p ⇒ s""" "${p.name}" -> ${p.name}""").mkString(",")} ))
       |""".stripMargin

}

case class ScalaRawOutput[T]() extends CompilationOutput[T] { compilation ⇒
  def apply(closure: ScalaClosure) = CompilationOutput.RawScala[T](closure)
  def wrapOutput = ""
}

trait ScalaWrappedCompilation <: ScalaCompilation { compilation ⇒

  type RETURN

  def compilationOutput: CompilationOutput[RETURN]
  def source: String
  def openMOLEImports = Seq(s"${CodeTool.namespace}._")
  def imports: Seq[String]

  def prefix = "_input_value_"

  def function(inputs: Seq[Prototype[_]]) =
    compile(script(inputs)).map { evaluated ⇒
      (evaluated, evaluated.getClass.getMethod("apply", classOf[Context], classOf[RandomProvider]))
    }

  def closure(inputs: Seq[Prototype[_]]) =
    function(inputs).map {
      case (evaluated, method) ⇒
        val closure: ScalaClosure = (context: Context, rng: RandomProvider) ⇒ method.invoke(evaluated, context, rng)
        compilationOutput(closure)
    }

  def toScalaNativeType(t: PrototypeType[_]): PrototypeType[_] = {
    def native = {
      val (contentType, level) = TypeUtil.unArrayify(t)
      for {
        m ← classEquivalence(contentType.runtimeClass).map(_.manifest)
      } yield (0 until level).foldLeft(PrototypeType.unsecure(m)) {
        (c, _) ⇒ c.toArray.asInstanceOf[PrototypeType[Any]]
      }
    }
    native getOrElse t
  }

  def script(inputs: Seq[Prototype[_]]) =
    (openMOLEImports ++ imports).map("import " + _).mkString("\n") + "\n\n" +
      s"""(${prefix}context: ${classOf[Context].getCanonicalName}, ${prefix}RNGProvider: ${classOf[RandomProvider].getCanonicalName}) => {
          |    object $inputObject {
          |      ${inputs.toSeq.map(i ⇒ s"""var ${i.name} = ${prefix}context("${i.name}").asInstanceOf[${toScalaNativeType(i.`type`)}]""").mkString("; ")}
          |    }
          |    import ${inputObject}._
          |    implicit lazy val ${Task.prefixedVariable("RNG")}: util.Random = ${prefix}RNGProvider()
          |    $source
          |    ${compilationOutput.wrapOutput}
          |}
          |""".stripMargin


  def run(context: Context)(implicit rng: RandomProvider) = compiled(context).get.run(context)(rng)

  def compiled(context: Context): Try[CompiledScala[RETURN]]
}


trait DynamicHeader { this: ScalaWrappedCompilation =>

  @transient lazy val cache = collection.mutable.HashMap[Seq[Prototype[_]], Try[CompiledScala[RETURN]]]()

  def compiled(context: Context): Try[CompiledScala[RETURN]] = {
    val contextPrototypes = context.toSeq.map { case (_, v) ⇒ v.prototype }
    compiled(contextPrototypes)
  }

  def compiled(inputs: Seq[Prototype[_]]): Try[CompiledScala[RETURN]] =
    cache.synchronized {
      val allInputMap = inputs.groupBy(_.name)

      val duplicatedInputs = allInputMap.filter { _._2.size > 1 }.map(_._2)

      duplicatedInputs match {
        case Nil ⇒
          def sortedInputNames = inputs.map(_.name).distinct.sorted
          val scriptInputs = sortedInputNames.map(n ⇒ allInputMap(n).head)
          cache.getOrElseUpdate(
            scriptInputs,
            closure(scriptInputs)
          )
        case duplicated ⇒ throw new UserBadDataError("Duplicated inputs: " + duplicated.mkString(", "))
      }
    }
}

trait StaticHeader { this: ScalaWrappedCompilation =>
  def inputs: Seq[Prototype[_]]
  @transient lazy val functionCode = closure(inputs)
  def compiled(context: Context): Try[CompiledScala[RETURN]] = functionCode
}