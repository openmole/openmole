package org.openmole.core.workflow.hook

import org.openmole.core.setter.DefinitionScope
import org.openmole.core.workflow.dsl.*

trait HookPackage:
  def CSVHook = org.openmole.core.workflow.hook.CSVHook
  val FromContextHook = org.openmole.core.workflow.hook.FromContextHook
  type FromContextHook = org.openmole.core.workflow.hook.FromContextHook

  export org.openmole.core.workflow.hook.HookDecorator

def Hook = FromContextHook
def display(implicit outputRedirection: org.openmole.tool.outputredirection.OutputRedirection): org.openmole.core.format.WritableOutput.Display = org.openmole.core.format.WritableOutput.Display(outputRedirection.output)

implicit class HookDecorator(h: Hook):
  infix def when(condition: Condition)(using DefinitionScope) = ConditionHook(h, condition)
  infix def when(condition: String)(using DefinitionScope) = ConditionHook(h, condition)
  infix def condition(condition: Condition)(using DefinitionScope) = when(condition)
  infix def condition(condition: String)(using DefinitionScope) = when(condition)
