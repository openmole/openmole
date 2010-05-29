/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.core.implementation.domain;

import org.openmole.core.model.domain.IInterval;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public abstract class Interval<T> implements IInterval<T> {
    private String min, max;

    public Interval(String min, String max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public String getMax() {
        return max;
    }

    @Override
    public String getMin() {
        return min;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public void setMin(String min) {
        this.min = min;
    }

}
