package org.openmole.core.workflow

package format {

  import org.openmole.tool.outputredirection._

  trait FormatPackage {
    type Display = WritableOutput.Display
    def display(implicit outputRedirection: OutputRedirection): Display = outputRedirection.output
    def CSVOutputFormat = format.CSVOutputFormat
  }
}

package object format {

}
