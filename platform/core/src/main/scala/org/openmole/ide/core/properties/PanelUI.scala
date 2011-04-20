/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import java.awt.Panel

trait PanelUI extends Panel{
  def name: String
  
  def entityType: Class[_]
}
