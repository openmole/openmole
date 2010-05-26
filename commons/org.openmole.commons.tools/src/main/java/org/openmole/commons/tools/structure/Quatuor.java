/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.commons.tools.structure;

/**
 *
 * @author reuillon
 */
public class Quatuor<FIRST,SECOND,THIRD,FORTH> {
    FIRST fisrt;
    SECOND second;
    THIRD third;
    FORTH forth;

    public Quatuor(FIRST fisrt, SECOND second, THIRD third, FORTH forth) {
        this.fisrt = fisrt;
        this.second = second;
        this.third = third;
        this.forth = forth;
    }

    public FIRST getFisrt() {
        return fisrt;
    }

    public FORTH getForth() {
        return forth;
    }

    public SECOND getSecond() {
        return second;
    }

    public THIRD getThird() {
        return third;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Quatuor<FIRST, SECOND, THIRD, FORTH> other = (Quatuor<FIRST, SECOND, THIRD, FORTH>) obj;
        if (this.fisrt != other.fisrt && (this.fisrt == null || !this.fisrt.equals(other.fisrt))) {
            return false;
        }
        if (this.second != other.second && (this.second == null || !this.second.equals(other.second))) {
            return false;
        }
        if (this.third != other.third && (this.third == null || !this.third.equals(other.third))) {
            return false;
        }
        if (this.forth != other.forth && (this.forth == null || !this.forth.equals(other.forth))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.fisrt != null ? this.fisrt.hashCode() : 0);
        hash = 97 * hash + (this.second != null ? this.second.hashCode() : 0);
        hash = 97 * hash + (this.third != null ? this.third.hashCode() : 0);
        hash = 97 * hash + (this.forth != null ? this.forth.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return fisrt.toString() + " " + second.toString() + " " + third.toString() + " " + forth.toString();
    }
}
