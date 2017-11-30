package org.openmole.core.workflow.tools

import java.io.PrintStream

object Progress {

  implicit def appyl(out: PrintStream = System.out) = new Progress {
    override def logger = out
  }

}

trait Progress {
  def logger: PrintStream
}

