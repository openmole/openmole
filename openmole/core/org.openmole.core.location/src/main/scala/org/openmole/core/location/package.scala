package org.openmole.core

import org.openmole.core.exception._
import java.io.File

package object location {
  def OpenMOLELocationProperty = "openmole.location"

  def openMOLELocationOption =
    Option(System.getProperty(OpenMOLELocationProperty, null)).map(new File(_))

  def openMOLELocation =
    openMOLELocationOption.getOrElse(throw new InternalProcessingError("openmole.location not set"))

}
