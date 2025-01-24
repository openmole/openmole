package org.openmole.core.setter

import monocle.{Iso, Lens}
import org.openmole.core.context.*
import monocle.Focus
import org.openmole.core.argument.{DefaultSet, FromContext}

trait InputBuilder[T]:
  def inputs: monocle.Lens[T, PrototypeSet]

trait OutputBuilder[T]:
  def outputs: monocle.Lens[T, PrototypeSet]

trait MappedInputBuilder[T]:
  def mappedInputs: monocle.Lens[T, Vector[ Mapped[?]]]

trait MappedOutputBuilder[T]:
  def mappedOutputs: monocle.Lens[T, Vector[ Mapped[?]]]

object DefaultBuilder:

  given DefaultBuilder[DefaultSet] with
    def defaults = Iso.id[DefaultSet]

  given[T]: DefaultBuilder[FromContext[T]] with
    def defaults = Focus[FromContext[T]](_.defaults)


trait DefaultBuilder[T]:
  def defaults: monocle.Lens[T, DefaultSet]

trait NameBuilder[T] { builder ⇒
  def name: monocle.Lens[T, Option[String]]
}

trait InputOutputBuilder[T] extends InputBuilder[T] with OutputBuilder[T] with DefaultBuilder[T]
trait MappedInputOutputBuilder[T] extends MappedInputBuilder[T] with MappedOutputBuilder[T]

object InputOutputBuilder:

  def apply[T](taskInfo: Lens[T, InputOutputConfig]) = new InputOutputBuilder[T]:
    override def inputs: Lens[T, PrototypeSet] = taskInfo andThen Focus[InputOutputConfig](_.inputs)
    override def defaults: Lens[T, DefaultSet] = taskInfo andThen Focus[InputOutputConfig](_.defaults)
    override def outputs: Lens[T, PrototypeSet] = taskInfo andThen Focus[InputOutputConfig](_.outputs)

object MappedInputOutputBuilder:

  def apply[T](mapped: Lens[T, MappedInputOutputConfig]) = new MappedInputOutputBuilder[T]:
    override def mappedInputs: Lens[T, Vector[ Mapped[?]]] = mapped andThen Focus[MappedInputOutputConfig](_.inputs)
    override def mappedOutputs: Lens[T, Vector[ Mapped[?]]] = mapped andThen Focus[MappedInputOutputConfig](_.outputs)


