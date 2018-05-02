package org.openmole.gui.plugin.wizard.jar

import org.openmole.gui.ext.data.{ SafePath, WizardData }

case class JarWizardData(embedAsPlugin: Boolean, plugin: Option[String] = None) extends WizardData
