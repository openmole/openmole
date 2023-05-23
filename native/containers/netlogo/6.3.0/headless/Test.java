import java.util.Optional;
import java.util.TreeMap;
import java.io.File;

public class Test {

    public static void main(String[] args) {
      try {
        var model = new File("ants.nlogo");

        var values = new TreeMap<String, Object>();
        values.put("gpopulation", 125.0);
        values.put("gdiffusion-rate", 10.0);
        values.put("gevaporation-rate", 10.0);
        values.put("gmax-steps", 2000.0);

        String[] outputs = { "final-ticks-food2" };

        String[] setup = { };
        String[] go = { "run-to-grid" };

        Optional<Integer> seed = Optional.of(42);

        var outputValues = Headless.run(model, values, outputs, setup, go, seed);
        for(Object v: outputValues) {
            System.out.println(v);
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    }
 
} 
