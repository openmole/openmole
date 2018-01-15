package org.openmole.plugin.task

import org.openmole.core.dsl._

package r {
  trait RPackage {

    lazy val rInputs = new {
      def +=(p: Val[_], name: String): RTask ⇒ RTask =
        (RTask.rInputs add (p, name)) andThen (inputs += p)
      def +=(p: Val[_]): RTask ⇒ RTask = +=(p, p.name)
    }

    lazy val rOutputs = new {
      def +=(name: String, p: Val[_]): RTask ⇒ RTask =
        (RTask.rOutputs add (name, p)) andThen (outputs += p)
      def +=(p: Val[_]): RTask ⇒ RTask = +=(p.name, p)
    }

  }
}

package object r extends RPackage
