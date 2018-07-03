package org.openmole.core.workflow.builder

import monocle.Lens
import monocle.macros.Lenses

@Lenses case class MappedInputOutputConfig(inputs: Vector[Mapped[_]] = Vector(), outputs: Vector[Mapped[_]] = Vector())
