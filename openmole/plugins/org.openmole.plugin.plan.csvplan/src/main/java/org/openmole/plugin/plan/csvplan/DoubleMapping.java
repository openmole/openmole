/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.plan.csvplan;

import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */
public class DoubleMapping implements IStringMapping<Object>{

    @Override
    public Object convert(String stringToBeConverted) throws UserBadDataError {
        return new Double(stringToBeConverted);
    }

}
