/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.tools.structure;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class Counter implements Comparable<Counter> {

    volatile Integer value;

    public Counter() {
        this(0);
    }

    public Counter(int value) {
        this.value = value;
    }

    public void increment() {
        value++;
    }

    public void decrement() {
        value--;
    }

    public Integer getValue() {
        return value;
    }

     public void increment(Integer val) {
        value+=val;
    }

    public void decrement(int val) {
        value-=val;
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Counter other = (Counter) obj;
        if (this.getValue() != other.getValue() && (this.getValue() == null || !this.getValue().equals(other.getValue()))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = this.getValue().hashCode();
        return hash;
    }



    public int compareTo(Counter intgr) {
        return getValue().compareTo(intgr.getValue());
    }





}
