package org.openmole.core.workflow

package hook {
  trait HookPackage {
    def CSVHook = hook.CSVHook
    val FromContextHook = hook.FromContextHook
    type FromContextHook = hook.FromContextHook
  }
}

package object hook {
  def Hook = FromContextHook
}
