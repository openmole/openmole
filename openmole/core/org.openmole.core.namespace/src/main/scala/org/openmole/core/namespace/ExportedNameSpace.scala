package org.openmole.core.namespace

object ExportedNameSpace {
  implicit def apply(p: Package): ExportedNameSpace = ExportedNameSpace(p.getName)
}

case class ExportedNameSpace(value: String)