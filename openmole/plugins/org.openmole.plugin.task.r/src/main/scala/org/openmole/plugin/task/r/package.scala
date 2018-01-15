package org.openmole.plugin.task

import org.openmole.core.dsl._

package r {
  trait RPackage {

    lazy val rInputs = new {
      def +=(p: Val[File], name: String): RTask ⇒ RTask =
        (RTask.rInputs add (p, name)) andThen (inputs += p)
    }

    lazy val rOutputs = new {
      def +=(name: String, p: Val[File]): RTask ⇒ RTask =
        (RTask.rOutputs add (name, p)) andThen (outputs += p)
    }

  }
}

package object r extends RPackage
