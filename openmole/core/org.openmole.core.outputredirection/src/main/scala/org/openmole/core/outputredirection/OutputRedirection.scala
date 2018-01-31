package org.openmole.core.outputredirection

object OutputRedirection {
  implicit def fromPrintStream(s: java.io.PrintStream) = OutputRedirection(s)
}

case class OutputRedirection(output: java.io.PrintStream = System.out)
