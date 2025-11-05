/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.core.setter

import org.openmole.core.context.*
import org.openmole.core.argument
import org.openmole.core.argument.*
import org.openmole.core.keyword.:=

/**
 * Part of the dsl for task properties (inputs, outputs, assignements)
 */

object Setter:
  implicit def setterToFunction[O, S](o: O)(implicit setter: Setter[O, S]): S => S = implicitly[Setter[O, S]].set(o)(_)
  implicit def seqOfSetterToFunction[O, S](o: Seq[O])(implicit setter: Setter[O, S]): S => S = Function.chain(o.map(o => implicitly[Setter[O, S]].set(o)(_)))

  given equalToAssignDefaultFromContext[T, U: DefaultBuilder as builder]: Setter[Val[T] := (FromContext[T], Boolean), U] =
    Setter: v =>
      builder.defaults.modify(_ + argument.Default[T](v.value, v.equal._1, v.equal._2))

  given equalToAssignDefaultFromContext2[T, U: DefaultBuilder as builder]: Setter[ValueAssignment[T], U] =
    Setter: v  =>
      builder.defaults.modify(_ + argument.Default[T](v.value, v.equal, false))

  given valueAssignmentFromUntyped[U: DefaultBuilder as builder]: Setter[ValueAssignment.Untyped, U] =
    Setter: v =>
      builder.defaults.modify(_ + argument.Default(v.value, v.equal, false))

  given equalToAssignDefaultValue[T, U: DefaultBuilder as builder]: Setter[Val[T] := (T, Boolean), U] =
    Setter: v =>
      builder.defaults.modify(_ + argument.Default[T](v.value, v.equal._1, v.equal._2))

  given equalToAssignDefaultValue2[T, U: DefaultBuilder as builder]: Setter[Val[T] := T, U] =
    Setter: v =>
      builder.defaults.modify(_ + argument.Default[T](v.value, v.equal, false))

  given equalToAssignDefaultSeqValue[T, U: DefaultBuilder]: Setter[Iterable[Val[T]] := (Iterable[T], Boolean), U] =
    Setter: v =>
      def defaults(u: U) = (v.value zip v.equal._1).foldLeft(u):
        case (u, (p, lv)) =>
          new :=(p, (lv, v.equal._2))(u)

      defaults


  def apply[O, T](f: O => T => T) = new Setter[O, T]:
    infix def set(o: O) = t => f(o)(t)


trait Setter[O, T]:
  infix def set(o: O): T => T


object Mapped:
  given [T]: Conversion[Val[T], Mapped[T]] = v => Mapped(v, None)

  def vals(xs: Iterable[ Mapped[?]]) = xs.map(_.v)

  def files(mapped: Vector[ Mapped[?]]) =
    mapped.collect:
      case m@Mapped(Val.caseFile(v), _) => Mapped[java.io.File](v, m.nameOption)


  def noFile(mapped: Vector[ Mapped[?]]) =
    mapped.flatMap:
      case Mapped(Val.caseFile(v), _) => Seq[ Mapped[?]]()
      case m                          => Seq(m)

/**
 * Prototype mapped to a variable name
 * @param v
 * @param name
 */
case class Mapped[T](v: Val[T], nameOption: Option[String]):
  def toTuple = v -> name
  def name = nameOption.getOrElse(v.name)

/**
 * Operations on inputs
 */
class Inputs:
  def +=[T: InputBuilder](d: Val[?]*): T => T =
    implicitly[InputBuilder[T]].inputs.modify(_ ++ d)
  def +=[T: MappedInputBuilder: InputBuilder](mapped:  Mapped[?]*): T => T =
    (this ++= Mapped.vals(mapped)) andThen implicitly[MappedInputBuilder[T]].mappedInputs.modify(_ ++ mapped)

  def ++=[T: InputBuilder](d: Iterable[Val[?]]*): T => T = +=[T](d.flatten *)
  def ++=[T: MappedInputBuilder: InputBuilder](mapped: Iterable[ Mapped[?]]*): T => T = +=[T](mapped.flatten *)

/**
 * Operations on outputs
 */
class Outputs:
  def +=[T: OutputBuilder](d: Val[?]*): T => T =
    implicitly[OutputBuilder[T]].outputs.modify(_ ++ d)

  def +=[T: MappedOutputBuilder: OutputBuilder](mapped:  Mapped[?]*): T => T =
    (this ++= Mapped.vals(mapped)) andThen implicitly[MappedOutputBuilder[T]].mappedOutputs.modify(_ ++ mapped)

  def ++=[T: OutputBuilder](d: Iterable[Val[?]]*): T => T = +=[T](d.flatten *)
  def ++=[T: MappedOutputBuilder: OutputBuilder](mapped: Iterable[ Mapped[?]]*): T => T = +=[T](mapped.flatten *)


class ExploredOutputs:
  def +=[T: OutputBuilder](ds: Val[_ <: Array[?]]*): T => T = (t: T) =>
    def outputs = implicitly[OutputBuilder[T]].outputs
    def add = ds.filter(d => !outputs.get(t).contains(d))
    (outputs.modify(_ ++ add) andThen outputs.modify(_.explore(ds.map(_.name) *)))(t)

  def ++=[T: OutputBuilder](d: Iterable[Val[_ <: Array[?]]]*): T => T = +=[T](d.flatten *)

class Defaults:
  def +=[U: DefaultBuilder](d: Default[?]*): U => U =
    implicitly[DefaultBuilder[U]].defaults.modify(_.toSeq ++ d)
  def ++=[T: DefaultBuilder](d: Iterable[Default[?]]*): T => T =
    +=[T](d.flatten *)

class Name:
  def :=[T: NameBuilder](name: String): T => T =
    implicitly[NameBuilder[T]].name.set(Some(name))

/**
 * DSL for i/o in itself
 */
trait BuilderPackage:
  final lazy val inputs: Inputs = new Inputs
  final lazy val outputs: Outputs = new Outputs
  final lazy val exploredOutputs: ExploredOutputs = new ExploredOutputs
  final lazy val defaults: Defaults = new Defaults

  /**
   * operators on both inputs and outputs
   * @param io
   */
  implicit class InputsOutputsDecorator(io: (Inputs, Outputs)):
    def +=[T: {InputBuilder, OutputBuilder}](ps: Val[?]*): T => T =
      (inputs.+=(ps*)) andThen (outputs.+=(ps*))
    def ++=[T: {InputBuilder, OutputBuilder}](ps: Iterable[Val[?]]*): T => T =
      (inputs.++=(ps*)) andThen (outputs.++=(ps*))

  export Setter.setterToFunction
  export Setter.seqOfSetterToFunction

  given equalToAssignDefaultSeqValue2[T, U: DefaultBuilder]: Setter[:=[Iterable[Val[T]], Iterable[T]], U] =
    Setter:  v =>
      def defaults(u: U) = (v.value zip v.equal).foldLeft(u) { case (u, (p, v)) => new:=(p, v)(u) }
      defaults

  /**
   * Construct mapped prototype
   * @param p
   * @tparam T
   */
  implicit class BuildMapped[T](p: Val[T]):
    /**
     * mapped to its own simple name
     * @return
     */
    def mapped: Mapped[T] = mapped(p.simpleName)

    /**
     * mapped to the given variable name
     * @param name
     * @return
     */
    infix def mapped(name: String) = Mapped(p, Some(name))

  final lazy val name = new Name

  implicit class SetBuilder[T](t: T):
    infix def set(ops: (T => T)*): T =
      ops.foldLeft(t) { (curT, op) => op(curT) }

  /**
   * Decorate a prototype with value assignements
   * @param v
   * @tparam T
   */
  implicit class ValueAssignmentDecorator[T](v: Val[T]):
    def :=(t: T): ValueAssignment[T] = new :=(v, t)
    def :=(t: FromContext[T]): ValueAssignment[T] = new :=(v, t)



object ValueAssignment:
  object Untyped:
    given [T]: Conversion[ValueAssignment[T], Untyped] = v => untyped(v)

  case class Untyped(v: Any):
    type T
    val assignment = v.asInstanceOf[ValueAssignment[T]]
    export assignment.*

  def untyped[TV](v: ValueAssignment[TV]) = 
    new Untyped(v):
      type T = TV

type ValueAssignment[T] = :=[Val[T], FromContext[T]]




