package org.openmole.ui.control;

/**
 *
 * @author mathieu
 */
public class TableType {

    public enum Name {
        INPUT_PARAMETER,
        OUTPUT_PARAMETER,
        DESIGN_OF_EXPERIMENT
    }

    public static String toString(TableType.Name tn) {
        if (tn == Name.INPUT_PARAMETER) return "Input parameters";
        else if (tn == Name.OUTPUT_PARAMETER) return "Output parameters";
        else if (tn == Name.DESIGN_OF_EXPERIMENT) return "Design of experiment";
        
        return "";

    }

}
