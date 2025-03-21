package org.openmole.tool.outputredirection

object OutputRedirection {
  implicit def fromPrintStream(s: java.io.PrintStream): OutputRedirection = OutputRedirection(s, s)
  def println(msg: String)(implicit outputRedirection: OutputRedirection) = outputRedirection.output.println(msg)
  def apply(output: java.io.PrintStream = System.out): OutputRedirection = OutputRedirection(output, output)
}

case class OutputRedirection(output: java.io.PrintStream, error: java.io.PrintStream)
