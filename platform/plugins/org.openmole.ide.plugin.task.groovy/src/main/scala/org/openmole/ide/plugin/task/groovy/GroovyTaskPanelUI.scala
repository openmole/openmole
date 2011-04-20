/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.task.groovy

import org.openmole.plugin.task.groovy.GroovyTask
import org.openmole.ide.core.properties.PanelUI

class GroovyTaskPanelUI extends PanelUI{
  override def name= "toto"
  
  override def entityType = classOf[GroovyTask] 
}
