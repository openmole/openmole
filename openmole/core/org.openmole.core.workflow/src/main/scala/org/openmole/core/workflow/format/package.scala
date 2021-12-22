package org.openmole.core.workflow.format

import org.openmole.tool.outputredirection._

trait FormatPackage {
  type Display = WritableOutput.Display
  def display(implicit outputRedirection: OutputRedirection): Display = WritableOutput.Display(outputRedirection.output)
  def CSVOutputFormat = org.openmole.core.workflow.format.CSVOutputFormat
}

