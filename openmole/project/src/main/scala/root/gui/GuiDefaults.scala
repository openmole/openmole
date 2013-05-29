package root.gui

import root.Defaults
import sbt._
import Keys._

trait GuiDefaults extends Defaults {
  def dir = file("gui")
  override val org = "org.openmole.ide"
}