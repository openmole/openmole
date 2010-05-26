/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.plugin.domain.relativerange;

import java.util.ArrayList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.tools.VariableExpansion;
import org.openmole.core.workflow.model.job.IContext;

public class RelativeRangeInteger extends RelativeRange<Integer> {

    public RelativeRangeInteger() {
        super("100", "5", "6");
    }

    public RelativeRangeInteger(String nominal, String percent, String size) {
        super(nominal, percent, size);
    }

    @Override
    public List<Integer> computeValues(IContext context) throws InternalProcessingError, UserBadDataError {
        Integer nominal = new Integer(VariableExpansion.getInstance().expandData(context, getNominal()));
        Integer percent = new Integer(VariableExpansion.getInstance().expandData(context, getPercent()));
        Integer size = new Integer(VariableExpansion.getInstance().expandData(context, getSize()));

        List<Integer> values = new ArrayList<Integer>(size);

        Integer min = nominal * (1 - percent / 100);
        if (size > 1) {
            Integer step = 2 * nominal * percent / 100 / (size - 1);
            for (int i = 0; i < size; i++) {
                values.add(min + i * step);
            }
        } else {
            values.add(min);
            values.add(nominal);
            values.add(nominal * (1 + percent / 100));
        }

        return values;
    }
}
