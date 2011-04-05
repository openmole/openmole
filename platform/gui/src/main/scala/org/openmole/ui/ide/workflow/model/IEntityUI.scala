/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.model

trait IEntityUI {
  def entityType: Class[_]= entityType
  
  def name: String= name
}

//public interface IEntityUI {
//    String getName();
//    Class getType();
//}
