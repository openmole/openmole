package org.openmole.plugin.hook.file

import monocle.Focus
import org.openmole.core.context.Context
import org.openmole.core.dsl.*
import org.openmole.core.argument.*
import org.openmole.core.setter.*
import org.openmole.core.workflow.hook.{Hook, HookExecutionContext}
import org.openmole.core.workflow.mole.*
import org.openmole.core.workflow.validation.*

import scala.annotation.implicitNotFound

object MatrixHook {

  implicit def isIO: InputOutputBuilder[MatrixHook] = InputOutputBuilder(Focus[MatrixHook](_.config))
  implicit def isInfo: InfoBuilder[MatrixHook] = InfoBuilder(Focus[MatrixHook](_.info))

  object ToMatrix {
    implicit def arrayInt: ToMatrix[Array[Int]] = a => a.toIterator.map(_.toString)
    implicit def arrayDouble: ToMatrix[Array[Double]] = a => a.toIterator.map(_.toString)
    implicit def arrayLong: ToMatrix[Array[Long]] = a => a.toIterator.map(_.toString)

    implicit def arrayArrayInt: ToMatrix[Array[Array[Int]]] = a => a.toIterator.map(_.mkString(","))
    implicit def arrayArrayLong: ToMatrix[Array[Array[Long]]] = a => a.toIterator.map(_.mkString(","))
    implicit def arrayArrayDouble: ToMatrix[Array[Array[Double]]] = a => a.toIterator.map(_.mkString(","))
  }

  @implicitNotFound("${T} is not a matrix")
  trait ToMatrix[T] {
    def apply(t: T): Iterator[String]
  }

  case class MatrixObject[T](prototype: Val[T], toMatrix: ToMatrix[T]) {
    def apply(context: Context) = toMatrix(context(prototype))
  }

  def apply[T](file: FromContext[File], matrix: Val[T])(implicit name: sourcecode.Name, definitionScope: DefinitionScope, toMatrix: ToMatrix[T]) =
    new MatrixHook(
      file = file,
      matrix = MatrixObject[T](matrix, toMatrix),
      config = InputOutputConfig(),
      info = InfoConfig()
    ) set (
      inputs += matrix
    )

}

case class MatrixHook(
  file:   FromContext[File],
  matrix: MatrixHook.MatrixObject[_],
  config: InputOutputConfig,
  info:   InfoConfig) extends Hook with ValidateHook {

  override def validate = file.validate

  override protected def process(executionContext: HookExecutionContext) = FromContext { parameters =>
    import parameters._

    val f = file.from(context)
    f.createParentDirectory
    f.content = ""

    for {
      line ← matrix(context)
    } f.append(line + "\n")

    context
  }

}
