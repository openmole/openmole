/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import javax.swing.JPanel

trait PanelUI extends JPanel{
  def coreObject(name: String): Object
}
