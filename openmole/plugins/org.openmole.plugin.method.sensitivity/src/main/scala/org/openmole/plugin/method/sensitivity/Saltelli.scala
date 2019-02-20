package org.openmole.plugin.method.sensitivity

import org.openmole.core.context._

object Saltelli {
  def firstOrder(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("firstOrder", output.name))
  def totalOrder(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("totalOrder", output.name))
}
