package org.openmole.core.workflow.hook

trait HookPackage {
  def CSVHook = org.openmole.core.workflow.hook.CSVHook
  val FromContextHook = org.openmole.core.workflow.hook.FromContextHook
  type FromContextHook = org.openmole.core.workflow.hook.FromContextHook
}

def Hook = FromContextHook

