package org.openmole.plugin.task

import org.openmole.core.dsl._

package r {

  import org.openmole.core.workflow.builder._

  trait RPackage {

    @deprecated
    lazy val rInputs = new {
      def +=[T: MappedInputBuilder: InputBuilder](p: Val[_], name: String): T ⇒ T = inputs += p mapped name
      def +=[T: MappedInputBuilder: InputBuilder](p: Val[_]*): T ⇒ T = inputs ++= p.map(_.mapped)
    }

    @deprecated
    lazy val rOutputs = new {
      def +=[T: MappedOutputBuilder: OutputBuilder](name: String, p: Val[_]): T ⇒ T = outputs += p mapped name
      def +=[T: MappedOutputBuilder: OutputBuilder](p: Val[_]*): T ⇒ T = outputs ++= p.map(_.mapped)
    }

  }
}

package object r extends RPackage
