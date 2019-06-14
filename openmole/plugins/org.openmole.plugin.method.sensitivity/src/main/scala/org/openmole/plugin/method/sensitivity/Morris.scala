package org.openmole.plugin.method.sensitivity

import org.openmole.core.context._

object Morris {
  def mu(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("mu", output.name))
  def muStar(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("muStar", output.name))
  def sigma(input: Val[_], output: Val[_]) = input.withNamespace(Namespace("sigma", output.name))
}
