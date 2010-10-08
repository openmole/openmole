/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.core.implementation.mole;

import org.openmole.core.model.job.ITicket;

public class Ticket implements ITicket, Comparable<ITicket> {

    private final String category;
    private final Long number;
    private final ITicket parent;

    Ticket(Long number, ITicket parent) {
        this.parent = parent;
        this.number = number;
        this.category = parent.getCategory();
    }

    Ticket(String category, Long number) {
        parent = this;
        this.category = category;
        this.number = number;
    }


    @Override
    public ITicket getParent() {
        return parent;
    }

    @Override
    public String getContent() {
        return number.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Ticket other = (Ticket) obj;
        if (this.number != other.number && (this.number == null || !this.number.equals(other.number))) {
            return false;
        }
        if ((this.category == null) ? (other.category != null) : !this.category.equals(other.category)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + (this.category != null ? this.category.hashCode() : 0);
        hash = 83 * hash + (this.number != null ? this.number.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean isRoot() {
        return getParent().equals(this);
    }

    @Override
    public int compareTo(ITicket o) {
        int compare = getContent().compareTo(o.getContent());
        if(compare != 0) return compare;
        return getCategory().compareTo(o.getCategory());
    }

    @Override
    public String getCategory() {
        return category;
    }
}
