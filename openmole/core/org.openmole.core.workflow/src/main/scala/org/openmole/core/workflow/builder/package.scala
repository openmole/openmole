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

package org.openmole.core.workflow

package builder {

  import org.openmole.core.context._
  import org.openmole.core.expansion._
  import org.openmole.core.workflow.tools._

  class Inputs {
    def +=[T: InputBuilder](d: Val[_]*): T ⇒ T =
      implicitly[InputBuilder[T]].inputs.modify(_ ++ d)
    def ++=[T: InputBuilder](d: Iterable[Val[_]]*): T ⇒ T =
      +=[T](d.flatten: _*)
  }

  class Outputs {
    def +=[T: OutputBuilder](d: Val[_]*): T ⇒ T =
      implicitly[OutputBuilder[T]].outputs.modify(_ ++ d)
    def ++=[T: OutputBuilder](d: Iterable[Val[_]]*): T ⇒ T =
      +=[T](d.flatten: _*)
  }

  class ExploredOutputs {
    def +=[T: OutputBuilder](ds: Val[_ <: Array[_]]*): T ⇒ T = (t: T) ⇒ {
      def outputs = implicitly[OutputBuilder[T]].outputs
      def add = ds.filter(d ⇒ !outputs.get(t).contains(d))
      (outputs.modify(_ ++ add) andThen outputs.modify(_.explore(ds.map(_.name): _*)))(t)
    }
    def ++=[T: OutputBuilder](d: Iterable[Val[_ <: Array[_]]]*): T ⇒ T =
      +=[T](d.flatten: _*)
  }

  class Defaults {
    def +=[U: DefaultBuilder](d: Default[_]*): U ⇒ U =
      implicitly[DefaultBuilder[U]].defaults.modify(_.toSeq ++ d)
    def ++=[T: DefaultBuilder](d: Iterable[Default[_]]*): T ⇒ T =
      +=[T](d.flatten: _*)
  }

  class Name {
    def :=[T: NameBuilder](name: String): T ⇒ T =
      implicitly[NameBuilder[T]].name.set(Some(name))
  }

  trait BuilderPackage {
    final lazy val inputs: Inputs = new Inputs
    final lazy val outputs: Outputs = new Outputs
    final lazy val exploredOutputs: ExploredOutputs = new ExploredOutputs
    final lazy val defaults: Defaults = new Defaults

    implicit class InputsOutputsDecorator(io: (Inputs, Outputs)) {
      def +=[T: InputBuilder: OutputBuilder](ps: Val[_]*): T ⇒ T =
        (inputs += (ps: _*)) andThen (outputs += (ps: _*))
      def ++=[T: InputBuilder: OutputBuilder](ps: Iterable[Val[_]]*): T ⇒ T =
        (inputs ++= (ps: _*)) andThen (outputs ++= (ps: _*))
    }

    class AssignDefault[T](p: Val[T]) {
      def :=[U: DefaultBuilder: InputBuilder](v: T, `override`: Boolean): U ⇒ U =
        (this := (v: FromContext[T], `override`)) andThen (inputs += p)
      def :=[U: DefaultBuilder: InputBuilder](v: T): U ⇒ U = this.:=(v, false)
      def :=[U: DefaultBuilder: InputBuilder](v: FromContext[T], `override`: Boolean): U ⇒ U =
        implicitly[DefaultBuilder[U]].defaults.modify(_ + Default[T](p, v, `override`)) andThen (inputs += p)
      def :=[U: DefaultBuilder: InputBuilder](v: FromContext[T]): U ⇒ U =
        this.:=(v, false)
    }

    class AssignDefaultSeq[T](p: Iterable[Val[T]]) {
      def :=[U: DefaultBuilder: InputBuilder](v: Iterable[T], `override`: Boolean): U ⇒ U = {
        def defaults(u: U) = (p zip v).foldLeft(u) { case (u, (p, v)) ⇒ (new AssignDefault(p).:=[U](v, `override`)).apply(u) }
        defaults _ andThen (inputs ++= p)
      }

      def :=[U: DefaultBuilder: InputBuilder](v: Iterable[T]): U ⇒ U = this.:=(v, false)
    }

    implicit def prototypeToAssignDefault[T](p: Val[T]) = new AssignDefault[T](p)

    final lazy val name = new Name

    implicit class SetBuilder[T](t: T) {
      def set(ops: (T ⇒ T)*): T =
        ops.foldLeft(t) { (curT, op) ⇒ op(curT) }
    }
  }

}

package object builder

