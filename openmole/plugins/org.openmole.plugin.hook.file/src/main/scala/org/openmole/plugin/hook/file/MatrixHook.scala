package org.openmole.plugin.hook.file

import monocle.macros.Lenses
import org.openmole.core.context.Context
import org.openmole.core.dsl._
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.hook.{ Hook, HookExecutionContext }
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation._

import scala.annotation.implicitNotFound

object MatrixHook {

  implicit def isIO: InputOutputBuilder[MatrixHook] = InputOutputBuilder(MatrixHook.config)
  implicit def isInfo = InfoBuilder(info)

  object ToMatrix {
    def apply[T](f: T ⇒ Iterator[String]) = new ToMatrix[T] {
      def apply(t: T) = f(t)
    }

    implicit def arrayInt = ToMatrix[Array[Int]] { a ⇒ a.toIterator.map(_.toString) }
    implicit def arrayDouble = ToMatrix[Array[Double]] { a ⇒ a.toIterator.map(_.toString) }
    implicit def arrayLong = ToMatrix[Array[Long]] { a ⇒ a.toIterator.map(_.toString) }

    implicit def arrayArrayInt = ToMatrix[Array[Array[Int]]] { a ⇒ a.toIterator.map(_.mkString(",")) }
    implicit def arrayArrayLong = ToMatrix[Array[Array[Long]]] { a ⇒ a.toIterator.map(_.mkString(",")) }
    implicit def arrayArrayDouble = ToMatrix[Array[Array[Double]]] { a ⇒ a.toIterator.map(_.mkString(",")) }
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

@Lenses case class MatrixHook(
  file:   FromContext[File],
  matrix: MatrixHook.MatrixObject[_],
  config: InputOutputConfig,
  info:   InfoConfig) extends Hook with ValidateHook {

  override def validate(inputs: Seq[Val[_]]) = Validate { p ⇒
    import p._
    file.validate(inputs)
  }

  override protected def process(executionContext: HookExecutionContext) = FromContext { parameters ⇒
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
