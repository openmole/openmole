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

package org.openmole.core.workflow.builder

import org.openmole.core.context.*
import org.openmole.core.expansion
import org.openmole.core.expansion.*
import org.openmole.core.keyword.:=
import org.openmole.core.workflow.tools.*
import scalaz.Alpha.T

/**
 * Part of the dsl for task properties (inputs, outputs, assignements)
 */

object Setter:
  def apply[O, T](f: O ⇒ T ⇒ T) = new Setter[O, T]:
    def set(o: O) = t => f(o)(t)

trait Setter[O, T]:
  def set(o: O): T => T

object IO:
  implicit def valToIO[T](v: Val[T]): RawVal[T] = RawVal(v)

  def collectVals(xs: Iterable[IO]) =
    xs.collect {
      case x: RawVal[_] ⇒ x.v
      case x: Mapped[_] ⇒ x.v
    }

  def collectMapped(xs: Iterable[IO]) =
    xs.collect { case x: Mapped[_] ⇒ x }

/**
 * Wrapper for prototypes as input/output
 */
sealed trait IO

/**
 * Single prototype
 * @param v
 */
case class RawVal[T](v: Val[T]) extends IO

object Mapped:
  def files(mapped: Vector[Mapped[_]]) =
    mapped.flatMap {
      case Mapped(Val.caseFile(v), name) ⇒ Seq(Mapped[java.io.File](v, name))
      case m                             ⇒ Seq()
    }

  def noFile(mapped: Vector[Mapped[_]]) =
    mapped.flatMap {
      case Mapped(Val.caseFile(v), _) ⇒ Seq[Mapped[_]]()
      case m                          ⇒ Seq(m)
    }

/**
 * Prototype mapped to a variable name
 * @param v
 * @param name
 */
case class Mapped[T](v: Val[T], name: String) extends IO:
  def toTuple = v -> name

/**
 * Operations on inputs
 */
class Inputs:
  def +=[T: InputBuilder](d: Val[_]*): T ⇒ T =
    implicitly[InputBuilder[T]].inputs.modify(_ ++ d)
  def +=[T: MappedInputBuilder: InputBuilder](mapped: IO*): T ⇒ T =
    (this ++= IO.collectVals(mapped)) andThen implicitly[MappedInputBuilder[T]].mappedInputs.modify(_ ++ IO.collectMapped(mapped))

  def ++=[T: InputBuilder](d: Iterable[Val[_]]*): T ⇒ T = +=[T](d.flatten: _*)
  def ++=[T: MappedInputBuilder: InputBuilder](mapped: Iterable[IO]*): T ⇒ T = +=[T](mapped.flatten: _*)

/**
 * Operations on outputs
 */
class Outputs:
  def +=[T: OutputBuilder](d: Val[_]*): T ⇒ T =
    implicitly[OutputBuilder[T]].outputs.modify(_ ++ d)

  def +=[T: MappedOutputBuilder: OutputBuilder](mapped: IO*): T ⇒ T =
    (this ++= IO.collectVals(mapped)) andThen implicitly[MappedOutputBuilder[T]].mappedOutputs.modify(_ ++ IO.collectMapped(mapped))

  def ++=[T: OutputBuilder](d: Iterable[Val[_]]*): T ⇒ T = +=[T](d.flatten: _*)
  def ++=[T: MappedOutputBuilder: OutputBuilder](mapped: Iterable[IO]*): T ⇒ T = +=[T](mapped.flatten: _*)


class ExploredOutputs:
  def +=[T: OutputBuilder](ds: Val[_ <: Array[_]]*): T ⇒ T = (t: T) ⇒ {
    def outputs = implicitly[OutputBuilder[T]].outputs
    def add = ds.filter(d ⇒ !outputs.get(t).contains(d))
    (outputs.modify(_ ++ add) andThen outputs.modify(_.explore(ds.map(_.name): _*)))(t)
  }

  def ++=[T: OutputBuilder](d: Iterable[Val[_ <: Array[_]]]*): T ⇒ T = +=[T](d.flatten: _*)

class Defaults:
  def +=[U: DefaultBuilder](d: Default[_]*): U ⇒ U =
    implicitly[DefaultBuilder[U]].defaults.modify(_.toSeq ++ d)
  def ++=[T: DefaultBuilder](d: Iterable[Default[_]]*): T ⇒ T =
    +=[T](d.flatten: _*)

class Name:
  def :=[T: NameBuilder](name: String): T ⇒ T =
    implicitly[NameBuilder[T]].name.set(Some(name))

/**
 * DSL for i/o in itself
 */
trait BuilderPackage {
  final lazy val inputs: Inputs = new Inputs
  final lazy val outputs: Outputs = new Outputs
  final lazy val exploredOutputs: ExploredOutputs = new ExploredOutputs
  final lazy val defaults: Defaults = new Defaults

  /**
   * operators on both inputs and outputs
   * @param io
   */
  implicit class InputsOutputsDecorator(io: (Inputs, Outputs)) {
    def +=[T: InputBuilder: OutputBuilder](ps: Val[_]*): T ⇒ T =
      (inputs.+=(ps*)) andThen (outputs.+=(ps*))
    def ++=[T: InputBuilder: OutputBuilder](ps: Iterable[Val[_]]*): T ⇒ T =
      (inputs.++=(ps*)) andThen (outputs.++=(ps*))
  }

  implicit def setterToFunction[O, S](o: O)(implicit setter: Setter[O, S]): S => S = implicitly[Setter[O, S]].set(o)(_)
  implicit def seqOfSetterToFunction[O, S](o: Seq[O])(implicit setter: Setter[O, S]): S => S = Function.chain(o.map(o => implicitly[Setter[O, S]].set(o)(_)))

  implicit def equalToAssignDefaultFromContext[T, U: DefaultBuilder]: Setter[:=[Val[T], (FromContext[T], Boolean)], U] =
    Setter[:=[Val[T], (FromContext[T], Boolean)], U] { v ⇒ implicitly[DefaultBuilder[U]].defaults.modify(_ + expansion.Default[T](v.value, v.equal._1, v.equal._2)) }

  implicit def equalToAssignDefaultFromContext2[T, U: DefaultBuilder]: Setter[:=[Val[T], FromContext[T]], U] =
    Setter[:=[Val[T], FromContext[T]], U] { v ⇒ implicitly[DefaultBuilder[U]].defaults.modify(_ + expansion.Default[T](v.value, v.equal, false)) }

  implicit def equalToAssignDefaultValue[T, U: DefaultBuilder]: Setter[:=[Val[T], (T, Boolean)], U] =
    Setter[:=[Val[T], (T, Boolean)], U] { v ⇒ implicitly[DefaultBuilder[U]].defaults.modify(_ + expansion.Default[T](v.value, v.equal._1: FromContext[T], v.equal._2)) }

  implicit def equalToAssignDefaultValue2[T, U: DefaultBuilder]: Setter[:=[Val[T], T], U] =
    Setter[:=[Val[T], T], U] { v ⇒ implicitly[DefaultBuilder[U]].defaults.modify(_ + expansion.Default[T](v.value, v.equal: FromContext[T], false)) }

  implicit def equalToAssignDefaultSeqValue[T, U: DefaultBuilder]: Setter[:=[Iterable[Val[T]], (Iterable[T], Boolean)], U] =
    Setter[:=[Iterable[Val[T]], (Iterable[T], Boolean)], U] { v ⇒
      def defaults(u: U) = (v.value zip v.equal._1).foldLeft(u) { case (u, (p, lv)) ⇒ new :=(p, (lv, v.equal._2))(u) }
      defaults _
    }

  implicit def equalToAssignDefaultSeqValue2[T, U: DefaultBuilder]: Setter[:=[Iterable[Val[T]], Iterable[T]], U] =
    Setter[:=[Iterable[Val[T]], Iterable[T]], U] { v ⇒
      def defaults(u: U) = (v.value zip v.equal).foldLeft(u) { case (u, (p, v)) ⇒ new :=(p, v)(u) }
      defaults _
    }

  /**
   * Construct mapped prototype
   * @param p
   * @tparam T
   */
  implicit class BuildMapped[T](p: Val[T]) {
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
    def mapped(name: String) = Mapped(p, name)
  }

  final lazy val name = new Name

  implicit class SetBuilder[T](t: T):
    def set(ops: (T ⇒ T)*): T =
      ops.foldLeft(t) { (curT, op) ⇒ op(curT) }

  /**
   * Decorate a prototype with value assignements
   * @param v
   * @tparam T
   */
  implicit class ValueAssignmentDecorator[T](v: Val[T]) {
    def :=(t: T): ValueAssignment[T] = new :=(v, t)
    def :=(t: FromContext[T]): ValueAssignment[T] = new :=(v, t)
  }
}

object DefinitionScope {
  case class Internal(name: String) extends DefinitionScope
  case object User extends DefinitionScope

  implicit def internal(scope: String): Internal = Internal(scope)

  object user {
    implicit def default: DefinitionScope = User
  }

}

sealed trait DefinitionScope

object ValueAssignment{

  object Untyped {
    implicit def converter[T](v: ValueAssignment[T]): Untyped = untyped(v)
  }

  case class Untyped(v: Any) {
    type T
    val assignment = v.asInstanceOf[ValueAssignment[T]]
    export assignment.*
  }

  def untyped[TV](v: ValueAssignment[TV]) = 
    new Untyped(v) {
      type T = TV
    }
} 
type ValueAssignment[T] = :=[Val[T], FromContext[T]]




