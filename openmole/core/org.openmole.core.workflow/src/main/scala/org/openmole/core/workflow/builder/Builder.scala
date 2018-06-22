package org.openmole.core.workflow.builder

import monocle.Lens
import org.openmole.core.context._
import org.openmole.core.workflow.tools._

trait InputBuilder[T] {
  def inputs: monocle.Lens[T, PrototypeSet]
}

trait OutputBuilder[T] {
  def outputs: monocle.Lens[T, PrototypeSet]
}

trait MappedInputBuilder[T] {
  def mappedInputs: monocle.Lens[T, Vector[Mapped[_]]]
}

trait MappedOutputBuilder[T] {
  def mappedOutputs: monocle.Lens[T, Vector[Mapped[_]]]
}

trait DefaultBuilder[T] {
  def defaults: monocle.Lens[T, DefaultSet]
}

trait NameBuilder[T] { builder ⇒
  def name: monocle.Lens[T, Option[String]]
}

trait InputOutputBuilder[T] extends InputBuilder[T] with OutputBuilder[T] with DefaultBuilder[T]
trait MappedInputOutputBuilder[T] extends MappedInputBuilder[T] with MappedOutputBuilder[T]

object InputOutputBuilder {

  def apply[T](taskInfo: Lens[T, InputOutputConfig]) = new InputOutputBuilder[T] {
    override def inputs: Lens[T, PrototypeSet] = taskInfo composeLens InputOutputConfig.inputs
    override def defaults: Lens[T, DefaultSet] = taskInfo composeLens InputOutputConfig.defaults
    override def outputs: Lens[T, PrototypeSet] = taskInfo composeLens InputOutputConfig.outputs
  }

}

object MappedInputOutputBuilder {

  def apply[T](mapped: Lens[T, MappedInputOutputConfig]) = new MappedInputOutputBuilder[T] {
    override def mappedInputs: Lens[T, Vector[Mapped[_]]] = mapped composeLens MappedInputOutputConfig.inputs
    override def mappedOutputs: Lens[T, Vector[Mapped[_]]] = mapped composeLens MappedInputOutputConfig.outputs
  }

}

