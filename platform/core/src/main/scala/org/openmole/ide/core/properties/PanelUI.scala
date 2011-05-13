/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import java.awt.event.ContainerListener
import javax.swing.JPanel
import java.awt.event.ContainerEvent

abstract class PanelUI(panelUIData: PanelUIData) extends JPanel with IPanelUI
