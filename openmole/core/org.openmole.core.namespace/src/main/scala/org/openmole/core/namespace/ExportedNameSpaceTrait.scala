package org.openmole.core.namespace

object ExportedNameSpaceTrait {
  implicit def apply(c: Class[_]): ExportedNameSpaceTrait = ExportedNameSpaceTrait(c.getCanonicalName)
}

case class ExportedNameSpaceTrait(value: String)