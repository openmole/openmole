/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ui.workflow.implementation;

import org.netbeans.api.visual.widget.Widget;

/**
 *
 * @author mathieu
 */
//public class JobUI extends ObjectViewUI<IJob> implements IJobUI<IJob> {
public class JobViewUI extends Widget {
    JobModelUI model;

    public JobViewUI(MoleScene scene) {
        super(scene);

        model = new JobModelUI();
    }
}
