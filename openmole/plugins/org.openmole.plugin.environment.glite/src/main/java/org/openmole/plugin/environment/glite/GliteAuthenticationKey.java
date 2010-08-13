/*
 *  Copyright (C) 2010 reuillon
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Affero GNU General Public License as published by
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

package org.openmole.plugin.environment.glite;

import org.openmole.core.model.execution.batch.IBatchServiceAuthenticationKey;

public class GliteAuthenticationKey implements IBatchServiceAuthenticationKey {

    String voName;
    String vomsURL;

    public GliteAuthenticationKey(String voName, String vomsURL) {
        super();
        this.voName = voName;
        this.vomsURL = vomsURL;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        final GliteAuthenticationKey other = (GliteAuthenticationKey) obj;
        if ((this.voName == null) ? (other.voName != null) : !this.voName.equals(other.voName)) {
            return false;
        }

        if ((this.vomsURL == null) ? (other.vomsURL != null) : !this.vomsURL.equals(other.vomsURL)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + (this.voName != null ? this.voName.hashCode() : 0);
        hash = 71 * hash + (this.vomsURL != null ? this.vomsURL.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return GliteAuthenticationKey.class.getName() + ": " + voName + "," + vomsURL;
    }



}
