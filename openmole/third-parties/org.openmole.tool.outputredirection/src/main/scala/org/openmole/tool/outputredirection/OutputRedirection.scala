package org.openmole.tool.outputredirection

object OutputRedirection {
  implicit def fromPrintStream(s: java.io.PrintStream) = OutputRedirection(s)
  def println(msg: String)(implicit outputRedirection: OutputRedirection) = outputRedirection.output.println(msg)
}

case class OutputRedirection(output: java.io.PrintStream = System.out)
