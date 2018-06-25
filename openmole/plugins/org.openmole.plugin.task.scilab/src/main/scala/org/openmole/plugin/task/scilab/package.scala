package org.openmole.plugin.task

import org.openmole.core.dsl._

package scilab {

  import org.openmole.core.workflow.builder._

  trait ScilabPackage {

    @deprecated
    lazy val scilabInputs = new {
      def +=[T: MappedInputBuilder: InputBuilder](p: Val[_], name: String): T ⇒ T = inputs += p mapped name
      def +=[T: MappedInputBuilder: InputBuilder](p: Val[_]*): T ⇒ T = inputs ++= p.map(_.mapped)
    }

    @deprecated
    lazy val scilabOutputs = new {
      def +=[T: MappedOutputBuilder: OutputBuilder](name: String, p: Val[_]): T ⇒ T = outputs += p mapped name
      def +=[T: MappedOutputBuilder: OutputBuilder](p: Val[_]*): T ⇒ T = outputs ++= p.map(_.mapped)
    }

  }
}

package object scilab extends ScilabPackage
