package org.openmole.plugin.task

import org.openmole.core.dsl._

package scilab {
  trait ScilabPackage {

    lazy val scilabInputs = new {
      def +=(p: Val[_], name: String): ScilabTask ⇒ ScilabTask =
        (ScilabTask.scilabInputs add (p, name)) andThen (inputs += p)
      def +=(p: Val[_]*): ScilabTask ⇒ ScilabTask = p.map(p ⇒ +=(p, p.name))
    }

    lazy val scilabOutputs = new {
      def +=(name: String, p: Val[_]): ScilabTask ⇒ ScilabTask =
        (ScilabTask.scilabOutputs add (name, p)) andThen (outputs += p)
      def +=(p: Val[_]*): ScilabTask ⇒ ScilabTask = p.map(p ⇒ +=(p.name, p))
    }

  }
}

package object scilab extends ScilabPackage
