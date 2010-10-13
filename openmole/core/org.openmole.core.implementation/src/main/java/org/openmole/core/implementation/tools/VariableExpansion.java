/*
 *
 *  Copyright (c) 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
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
package org.openmole.core.implementation.tools;

import groovy.lang.Binding;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.openmole.core.model.data.IVariable;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.groovy.GroovyProxy;
import org.openmole.core.implementation.job.Context;
import org.openmole.core.model.job.IContext;

public class VariableExpansion {

    final protected Character patternBegin = '{';
    final protected Character patternEnd = '}';
    final protected String eval = "$" + patternBegin;
    protected static VariableExpansion instance;

    protected VariableExpansion() {
    }

    public static VariableExpansion getInstance() {
        if (instance != null) {
            return instance;
        }

        synchronized (VariableExpansion.class) {
            if (instance == null) {
                instance = new VariableExpansion();
            }
            return instance;
        }
    }

    public static BigDecimal expandBigDecimalData(IContext global, IContext context, String s) throws InternalProcessingError, UserBadDataError {
        return new BigDecimal(expandData(global, context, s));
    }

    public static double expandDoubleData(IContext global, IContext context, String s) throws InternalProcessingError, UserBadDataError {
        return new Double(expandData(global, context, s));
    }

    public static int expandIntegerData(IContext global, IContext context, String s) throws InternalProcessingError, UserBadDataError {
        return new Integer(expandData(global, context, s));
    }

    public static String expandData(IContext global, IContext context, String s) throws InternalProcessingError, UserBadDataError {
        return expandData(global, context, Collections.EMPTY_LIST, s);
    }

    public static String expandData(IContext global, IContext context, List<IVariable> tmpVariable, String s) throws InternalProcessingError, UserBadDataError {
        return getInstance().expandDataInernal(global, context, tmpVariable, s);
    }

    public String expandDataInernal(IContext global, IContext context, List<IVariable> tmpVariable, String s) throws InternalProcessingError, UserBadDataError {
        String ret = s;

        IContext allVariables = new Context();
        allVariables.putVariables(context);
        allVariables.putVariables(tmpVariable);

        do {
            int beginIndex = ret.indexOf(eval);

            int cur = beginIndex + 2;
            int curLevel = 0;
            int retLenght = ret.length();

            while (cur < retLenght) {
                char curChar = ret.charAt(cur);

                if (curChar == patternEnd) {
                    if (curLevel == 0) {
                        break;
                    }
                    curLevel--;
                }
                if (curChar == patternBegin) {
                    curLevel++;
                }
                cur++;
            }

            if (cur < retLenght) {
                String toInsert = expandOneData(global, allVariables, getVarName(ret.substring(beginIndex + 1, cur + 1)));
                ret = ret.substring(0, beginIndex) + toInsert + ret.substring(cur + 1);
            } else {
                break;
            }
        } while (true);


        return ret;

    }

    public static String expandData(ReplacementSet replace, List<IVariable> tmpVariable, String s) {
        return getInstance().expandDataInternal(replace, tmpVariable, s);
    }

    public String expandDataInternal(ReplacementSet replace, List<IVariable> tmpVariable, String s) {
        String ret = s;

        do {
            int beginIndex = ret.indexOf(patternBegin);
            int endIndex = ret.indexOf(patternEnd);

            if (beginIndex == -1 || endIndex == -1) {
                break;
            }
            String toReplace = ret.substring(beginIndex, endIndex + 1);

            String varName = getVarName(toReplace);

            if (replace.containsKey(varName)) {
                String toInsert = replace.get(varName);
                ret = ret.substring(0, beginIndex) + toInsert + ret.substring(endIndex + 1);
            } else {
                ret = ret.substring(0, beginIndex) + ret.substring(endIndex + 1);
            }
        } while (true);


        return ret;
    }

    private String getVarName(String str) {
        return str.substring(1, str.length() - 1);
    }

    protected String expandOneData(IContext global, IContext allVariables, String variableExpression) throws InternalProcessingError, UserBadDataError {
        IVariable variable = allVariables.getVariable(variableExpression);
        if (variable != null) {
            return variable.getValue().toString();
        }

        GroovyShellProxyAdapter shell = new GroovyShellProxyAdapter(new GroovyProxy());
        Binding binding = GroovyShellProxyAdapter.fromContextToBinding(global, allVariables);

        shell.compile(variableExpression, Collections.EMPTY_LIST);

        String ret = shell.execute(binding).toString();
        return ret;
    }
}
