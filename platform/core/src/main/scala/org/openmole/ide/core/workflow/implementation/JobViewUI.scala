/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.netbeans.api.visual.widget.Widget

class JobViewUI(scene: MoleScene, val jobModel: Option[JobModelUI]= None) extends Widget(scene)
//
// extends Widget {
//    JobModelUI model;
//
//    public JobViewUI(MoleScene scene) {
//        super(scene);
//
//        model = new JobModelUI();
//    }