package org.openmole.core.workflow.builder

case class MappedInputOutputConfig(inputs: Vector[Mapped[_]] = Vector(), outputs: Vector[Mapped[_]] = Vector())
