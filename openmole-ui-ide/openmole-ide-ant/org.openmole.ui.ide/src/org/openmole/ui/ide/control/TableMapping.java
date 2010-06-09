package org.openmole.ui.ide.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.openmole.ui.ide.control.TableType.Name;

/**
 *
 * @author mathieu
 */
public class TableMapping {

    private static TableMapping instance = null;

    public static final String TYPE = "Type";
    public static final String PARAMETER = "Parameter";
    public static final String VALUE = "Value";
    public static final String OPTIONAL = "Optional";
    public static final String FACTOR = "Factor";
    public static final String BOUND_MIN = "Bound inf";
    public static final String BOUND_MAX = "Bound sup";
    
    private Map<Name, Collection<String>> tableMap = new HashMap();


    public Collection<String> getHeaders(TableType.Name tn){
        return tableMap.get(tn);
    }

    public void initialize() {
        //Input parmaters
        Collection<String> li = new ArrayList<String>();
        li.add(TYPE);
        li.add(PARAMETER);
        li.add(VALUE);
        li.add(OPTIONAL);
        tableMap.put(Name.INPUT_PARAMETER, li);

        //output parmaters
        tableMap.put(Name.OUTPUT_PARAMETER, li);

        //design of experiment
        Collection<String> lidod = new ArrayList<String>();
        lidod.add(FACTOR);
        lidod.add(BOUND_MIN);
        lidod.add(BOUND_MAX);
        tableMap.put(Name.DESIGN_OF_EXPERIMENT, lidod);

    }

    public static TableMapping getInstance() {
        if (instance == null) {
            instance = new TableMapping();
        }
        return instance;
    }
}
