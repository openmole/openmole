package org.openmole.core.workflow.builder

import monocle.{Lens, Iso}
import org.openmole.core.context.*
import org.openmole.core.workflow.tools.*
import monocle.Focus
import org.openmole.core.expansion.{DefaultSet, FromContext}

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

object DefaultBuilder {

  implicit def defaultSetDefaultBuilder: DefaultBuilder[DefaultSet] & InputBuilder[DefaultSet] = new DefaultBuilder[DefaultSet] with InputBuilder[DefaultSet] {
    override def defaults: Lens[DefaultSet, DefaultSet] = Iso.id
    override def inputs: Lens[DefaultSet, PrototypeSet] = Lens { (_: DefaultSet) ⇒ PrototypeSet.empty } { p ⇒ d ⇒ d }
  }

  implicit def fromContextDefaultBuilder[T]: DefaultBuilder[FromContext[T]] & InputBuilder[FromContext[T]] = new DefaultBuilder[FromContext[T]] with InputBuilder[FromContext[T]]  {
    override def defaults: Lens[FromContext[T], DefaultSet] = Focus[FromContext[T]](_.defaults)
    override def inputs: Lens[FromContext[T], PrototypeSet] = Lens { (_: FromContext[T]) ⇒ PrototypeSet.empty } { p ⇒ d ⇒ d }
  }

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
    override def inputs: Lens[T, PrototypeSet] = taskInfo andThen Focus[InputOutputConfig](_.inputs)
    override def defaults: Lens[T, DefaultSet] = taskInfo andThen Focus[InputOutputConfig](_.defaults)
    override def outputs: Lens[T, PrototypeSet] = taskInfo andThen Focus[InputOutputConfig](_.outputs)
  }

}

object MappedInputOutputBuilder {

  def apply[T](mapped: Lens[T, MappedInputOutputConfig]) = new MappedInputOutputBuilder[T] {
    override def mappedInputs: Lens[T, Vector[Mapped[_]]] = mapped andThen Focus[MappedInputOutputConfig](_.inputs)
    override def mappedOutputs: Lens[T, Vector[Mapped[_]]] = mapped andThen Focus[MappedInputOutputConfig](_.outputs)
  }

}

