// //> using repository https://dl.cloudsmith.io/public/netlogo/netlogo/maven/
// //> using dep "org.nlogo:netlogo:6.3.0,exclude=org.jogamp.jogl%jogl-all,exclude=org.jogamp.gluegen%gluegen-rt"
//> using dep "com.thoughtworks.xstream:xstream:1.4.20"

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import org.nlogo.agent.Observer;
import org.nlogo.agent.World;
import org.nlogo.api.LogoException;
import org.nlogo.api.LogoListBuilder;
import org.nlogo.core.LogoList;
import org.nlogo.headless.HeadlessWorkspace;

import com.thoughtworks.xstream.*;
import com.thoughtworks.xstream.io.binary.*;


public class Headless {

    public static void main(String[] args) {
        try {
            var inputFile = args[0];

            var outputFile = args[1];

            var serializer = new XStream(null, new BinaryStreamDriver());
            var inputs = (Object[]) serializer.fromXML(new File(inputFile));

            var model = new File((String) inputs[0]);
            var values = (Map<String, Object>) inputs[1];
            var outputs = (String[]) inputs[2];
            var setup = (String[]) inputs[3];
            var go = (String[]) inputs[4];
            var seed = (Optional<Integer>) inputs[5];

            var outputValues = Headless.run(model, values, outputs, setup, go, seed);
            try (var fos = new FileOutputStream(outputFile)) {
                serializer.toXML(outputValues, fos);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }

//        var values = new TreeMap<String, Object>();
//        values.put("gpopulation", 125.0);
//        values.put("gdiffusion-rate", 10.0);
//        values.put("gevaporation-rate", 10.0);
//        values.put("gmax-steps", 2000.0);
//
//        String[] outputs = { "final-ticks-food2" };
//
//        String[] setup = { };
//        String[] go = { "run-to-grid" };
//
//        Optional<Long> seed = Optional.of(42L);
//
//        var outputValues = Headless.run(model, values, outputs, setup, go, seed);
//        for(Object v: outputValues) {
//            System.out.println(v);
//        }
    }

    private static LogoList arrayToList(Object[] array){
        LogoListBuilder list = new LogoListBuilder();
        for(Object o: array){
            if(o instanceof Object[]){list.add(arrayToList((Object[]) o));}
            else{list.add(o);}
        }
        return(list.toLogoList());
    }


    private static AbstractCollection listToCollection(LogoList list){
        AbstractCollection collection = new LinkedList();
        for(Object o:list.toJava()){
            if(o instanceof LogoList){collection.add(listToCollection((LogoList) o));}
            else{collection.add(o);}
        }
        return(collection);
    }

    public static void setGlobal(HeadlessWorkspace workspace, String variable, Object value) throws Exception {
        if(value instanceof Object[]){
            workspace.world().setObserverVariableByName(variable,arrayToList((Object[]) value));
        }
        else{
            workspace.world().setObserverVariableByName(variable,value);
        }
    }

    public static Object report(HeadlessWorkspace workspace, String variable) throws Exception {
        Object result = workspace.report(variable);
        if(result instanceof LogoList){
            return listToCollection((LogoList) result);
        } else {
            return result;
        }
    }

    public static Object[] run(File model, Map<String, Object> inputValues, String[] outputNames, String[] setup, String[] go, Optional<Integer> seed) throws Exception {
        var workspace = HeadlessWorkspace.newInstance();
        try {
            workspace.open(model.getPath());
            for(Integer s: seed.stream().toList()) {
                workspace.command("random-seed " + s);
            }

            for(String cmd: setup) {
                workspace.command(cmd);
            }

            for(Map.Entry<String, Object> entry : inputValues.entrySet()) {
                setGlobal(workspace, entry.getKey(), entry.getValue());
            }

            for(String cmd: go) {
                workspace.command(cmd);
            }

            var outputValues = new ArrayList<Object>();
            for(String name: outputNames) {
                var value = report(workspace, name);
                outputValues.add(value);
            }

            return outputValues.toArray(new Object[0]);
        } finally {
            workspace.dispose();
        }
    } 
}

