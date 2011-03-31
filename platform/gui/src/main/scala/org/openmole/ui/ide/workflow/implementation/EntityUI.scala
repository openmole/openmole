/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.workflow.implementation

import org.openmole.ui.ide.workflow.model.IEntityUI

class EntityUI(name: String, entityType: Class[_]) extends IEntityUI {}
//
//public class EntityUI implements IEntityUI {
//    private String name;
//    private Class type;
//
//    public EntityUI(){
//        this.name = "";
//        this.type = null;
//    }
//    
//    public EntityUI(String name, Class type) {
//        this.name = name;
//        this.type = type;
//    }
//
//    @Override
//    public String getName() {
//        return name;
//    }
//
//    @Override
//    public Class getType() {
//        return type;
//    }
//}