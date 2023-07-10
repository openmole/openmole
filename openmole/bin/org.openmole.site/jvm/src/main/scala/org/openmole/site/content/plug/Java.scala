package org.openmole.site.content.plug

/*
 * Copyright (C) 2023 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.site.content.header.*

object Java extends PageContent(html"""

${h2("Use a JavaTask")}

${h3{"Preliminary remarks"}}

${Native.preliminary("JavaTask")}


${h3{"Arguments of the JavaTask"}}

It takes the following arguments:

${ul(
   li{html"${code{"script"}} String or file, $mandatory. The Scilab script to be executed."},
   li{html"${code{"install"}} Sequence of strings, $optional (default = empty). The commands to be executed prior to the script execution (to install libraries on the system)."},
   li{html"${code{"jars"}} Sequence of File, $optional. The some jars to add in the classpath of JVM."},
   li{html"${code{"libraries"}} Sequence of String, $optional. The some libraries to use, the format in the one of scala-cli, for instance \"com.github.pathikrit::better-files:3.9.2\"."},
   li{html"${code{"jvmOptions"}} Sequence of String, $optional. The some options to pass to the JVM, for instance \"-Xmx1G\"."},
   li{html"${code{"version"}} String, $optional. The version of JVM to run."},
   li{html"${code{"prepare"}} Sequence of strings, $optional (default = empty). System commands to be executed just before to the execution of Java on the execution node."},
)}

${h3{"Simple JavaTask"}}

Here is a workflow calling a function found in a jar file using a ${code{"JavaTask"}}:

$br$br

${hl.openmole("""
val name = Val[String]
val output = Val[String]

val hello = JavaTask("val output = hellocode.Hello(name)", jars = Seq(workDirectory / "hellocode_3-1.0.jar")) set (
  inputs += name.mapped,
  outputs += output.mapped)

DirectSampling(
  sampling = name in List("Richard", "Martin", "Benjamin"),
  evaluation = hello
) hook display
""")}

${h2("Use a ScalaTask to plug Java code")}

OpenMOLE makes the inclusion of your own Java code in a workflow simple.
Since the Scala compiler can compile Java code, Java code can be directly fed to a ${code{"ScalaTask"}} as shown in ${aa("the ScalaTask example", href := DocumentationPages.scala.file)}.
Additionally, a compiled Java program can be encapsulated in a ${code{"ScalaTask"}} as a jar file through the ${code{"libraries"}} parameter as below.

$br$br

In this page, we present two examples on how to execute Java code in OpenMOLE.
The first one works for simple Java programs and the second one for a more complex program that requires dependencies managed by Maven.
For more ambitious developments, it is recommended to consider embedding your code in an ${a("OpenMOLE plugin", href := DocumentationPages.pluginDevelopment.file)}.
It is pretty straightforward since they are simple ${a("OSGi", href:=shared.link.osgi)} bundles.



${h2{"Simple Java Program"}}
${h3{"One input, no output"}}

Let's consider the simple code ${code{"Hello.java"}} in a directory named ${i{"hello"}} (to respect Java's package structure):

$br$br

${hl("""
package hello;

public class Hello {
    public static void run(int arg) {
        System.out.println("Hello from Java! " + arg);
    }
}""", "java")}

$br

We compile the code and generate the JAR file as follows:

$br$br

${hl("""
mkdir hello
mv Hello.java hello
cd hello
javac Hello.java
cd ..
jar cvf Hello.jar hello""", "plain")}

$br

To call this file, we will use a ${code{"ScalaTask"}}, which is able to execute Java code.
In order to do that, you should first create a folder in the OpenMOLE interface, and then upload the ${i{"Hello.jar"}} file.
In the same folder, you may then write an OMS script such as:

$br$br

${hl.openmole("""
// Declare variables
val proto1 = Val[Int]

//Define a task to perform the hello function
val javaTask = ScalaTask("hello.Hello.run(proto1)") set (
    libraries += (workDirectory / "Hello.jar"),
    inputs += proto1
)

// Workflow
DirectSampling(
    evaluation = javaTask,
    sampling = proto1 in (0 until 10)
)
""")}

$br

The output should look like that (the order in which the lines are printed might be different in your case):

$br$br

${hl("""
  Hello from Java! 0
  Hello from Java! 1
  Hello from Java! 2
  Hello from Java! 3
  Hello from Java! 4
  Hello from Java! 5
  Hello from Java! 6
  Hello from Java! 7
  Hello from Java! 8
  Hello from Java! 9
  Hello from Java! 10
""", "plain")}


${h3{"Two inputs, two outputs"}}

In the general case, a task is used to compute some output values depending on some input values.
To illustrate that, let's consider another Java code:

$br$br

${hl("""
package hello;

public class Hello {
  public static double[] run(double arg1, double arg2) {
    return double[]{arg1 * 10, arg2 * 10};
  }
}""", "java")}

$br

You can compile it and package as "Hello.jar" in the same manner as the previous example.
You can then define the ${code{"ScalaTask"}} input values when calling the Java function, and assign the function results to the task output variables:

$br$br

${hl.openmole("""
// Declare variables
val arg1 = Val[Double]
val arg2 = Val[Double]
val out1 = Val[Double]
val out2 = Val[Double]

// Declare a task
val javaTask = ScalaTask("Array(out1, out2) = hello.Hello.run(arg1, arg2)") set (
    libraries += (workDirectory / "Hello.jar"),
    inputs += (arg1, arg2),
    outputs += (arg1, arg2, out1, out2)
)

// Save the result in a CSV file
val csvHook = AppendToCSVFileHook(workDirectory / "result.csv")

DirectSampling(
    evaluation = javaTask hook csvHook,
    sampling =
        (arg1 in (0.0 to 10.0 by 1.0)) x
        (arg2 in (0.0 until 10.0 by 1.0))
)
""")}

$br

This workflow will call the function ${code{"hello"}} with both arguments varying from 0 to 10, and save the results in a CSV file.
Variables ${code{"arg1"}} and ${code{"arg2"}} are also specified as task outputs so that they are also written to the CSV file by the hook (see ${aa("Hooks", href := DocumentationPages.hook.file)}).



${h2("Example of Java code working with files")}

When a task needs to access external files (read from or write to), OpenMOLE needs to know it so it can pass them around to distant computing nodes.
Let's consider another "Hello World" code ${i{"Hello.java"}}.
This program reads the content of a file and writes it to another file.

$br$br

${hl("""
    package hello;

    import java.io.*;

    public class Hello {

      public static void run(int arg, File input, File output) throws IOException {
        //Read the input file
        String content = readFileAsString(input);
        PrintStream myStream = new PrintStream(new FileOutputStream(output));
        try {
          myStream.println(content + "  " + arg);
        } finally {
          myStream.close();
        }
      }

      private static String readFileAsString(File file) throws IOException {
        byte[] buffer = new byte[(int) file.length()];
        BufferedInputStream f = null;
        try {
          f = new BufferedInputStream(new FileInputStream(file));
          f.read(buffer);
        } finally {
          if (f != null) try { f.close(); } catch (IOException ignored) { }
        }
        return new String(buffer);
      }
    }""", "java")}

$br

You can compile it and package as ${i{"Hello.jar"}} in the same manner as the previous example.
For the program to access external files, specify them to the task as ${code{"inputFile"}}.

$br$br

${hl.openmole(s"""
    // Declare variables
    val proto1 = Val[Int]
    val inputFile = Val[File]
    val outputFile = Val[File]

    // Define the scala task as a launcher of the hello executable
    val javaTask = ScalaTask($tq
        val outputFile = newFile()
        hello.Hello.run(proto1, inputFile, outputFile)
    $tq) set (
        libraries += (workDirectory / "Hello.jar"),
        inputs += (proto1, inputFile),
        outputs += (proto1, outputFile),

        // Default value
        inputFile := (workDirectory / "input.txt")
    )

    // Save the output file locally
    val copyHook = CopyFileHook(
        outputFile,
        (workDirectory / "out-$${proto1}.txt")
    )

    // Workflow
    DirectSampling(
        evaluation = javaTask hook copyHook,
        sampling = proto1 in (0 to 10)
    )
""")}

$br

For this example to work you should create a file named "input.txt" in the work directory of your project.


${h2("Building a bundle with Maven")}

OpenMOLE can use complex Java libraries as they are well packaged.
Maven, that manages Java project life-cycle and dependencies, has a plugin (maven-shade) that can produce such bundles.
In order to allow the use of this plugin, you have to configure it in the plugin list of the maven project file.
In the ${em{"<plugins> </plugins>"}} section, you have to add the following code:

$br$br

${hl(s"""
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>2.4.3</version>
    <executions>
        <execution>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <filters>
                    <filter>
                        <artifact>*:*</artifact>
                        <excludes>
                            <exclude>META-INF/*.SF</exclude>
                            <exclude>META-INF/*.DSA</exclude>
                            <exclude>META-INF/*.RSA</exclude>
                        </excludes>
                    </filter>
                </filters>
                <shadedArtifactAttached>true</shadedArtifactAttached>
                <!-- This bit merges the various META-INF/services files. -->
                <transformers>
                    <transformer
                  implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer" />
                </transformers>
            </configuration>
        </execution>
    </executions>
</plugin>""", "xml")}

$br

Once this code is added, you can produce the  bundle by simply executing a build command (with ${em{"mvn install"}} for example), and it will be created into the ${em{"target"}} directory.
The file is named ${i{"name_of_youar_artifact-version-shaded.jar"}}.
You can now upload it into the OpenMOLE client and use it as described in the tutorial for simple Java code.

${h2("Troubleshooting using raster data from GeoTools")}

Several libraries, such as GeoTools, use JAI library that embeds a provider mechanism to load the different functions.
In order to use it properly, notably for calculation distribution it requires (1) to declare explicitly the use of JAI in the JAva code and (2) to ensure in the OpenMOLE script that all the classes are used in the same class loader.
You can refer to a simple project that includes a very simple task that handles raster data in the  ${a("following GitHub repository", href := "https://github.com/mbrasebin/test_geotools_openmole")}


${h3("Declaration of JAI")}

In order to declare the JAI functions, a code has to be prepared such as following ${em{"initJai()}"}} method that is defined for a TiffReaderTask class.

$br$br

${hl("""
    public static void initJAI() {
         // Disable mediaLib searching that produces unwanted errors
         // See https://www.java.net/node/666373
         System.setProperty("com.sun.media.jai.disableMediaLib", "true");

         // As the JAI jars are bundled in the geotools plugin, JAI initialization does not work,
         // so we need to perform the tasks described here ("Initialization and automatic loading of registry objects"):
         // http://docs.oracle.com/cd/E17802_01/products/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/OperationRegistry.html
         OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
         if (registry == null) {
             System.out.println("geotools: error in JAI initialization. Cannot access default operation registry");
         } else {
             // Update registry with com.sun.media.jai.imageioimpl.ImageReadWriteSpi (only class listed javax.media.jai.OperationRegistrySpi)
             // it would be safer to parse this file instead, but a JAI update is very unlikely as it has not been modified since 2005
             try {
                 new ImageReadWriteSpi().updateRegistry(registry);
             } catch (IllegalArgumentException e) {
                 // See #10652: IAE: A descriptor is already registered against the name "ImageRead" under registry mode "rendered"
                 System.out.println("GeoTools: error in JAI/ImageReadWriteSpi initialization: "+e.getMessage());
             }

             // Update registry with GeoTools registry file
             try (InputStream in = TiffReaderTask.class.getResourceAsStream("/META-INF/registryFile.jai")) {
                 if (in == null) {
                     System.out.println("geotools: error in JAI initialization. Cannot access META-INF/registryFile.jai");
                 } else {
                     registry.updateFromStream(in);
                 }
             } catch (IOException | IllegalArgumentException e) {
                 System.out.println("GeoTools: error in JAI/GeoTools initialization: "+e.getMessage());
             }
         }

         // Manual registering because plugin jar is not on application classpath
         IIORegistry ioRegistry = IIORegistry.getDefaultInstance();
         ClassLoader loader = TiffReaderTask.class.getClassLoader();

         Iterator<Class<?>> categories = ioRegistry.getCategories();
         while (categories.hasNext()) {
             @SuppressWarnings("unchecked")
             Iterator<IIOServiceProvider> riter = ServiceLoader.load((Class<IIOServiceProvider>) categories.next(), loader).iterator();
             while (riter.hasNext()) {
                 IIOServiceProvider provider = riter.next();
                 System.out.println("Registering " + provider.getClass());
                 ioRegistry.registerServiceProvider(provider);
             }
         }
 }
""", "java")}

$br

This function simply has to be called at the beginning of the task code.
For example for the TiffReaderTask:

$br$br

${hl("""
   public static double readGeoTiff(File file) throws IOException {

   System.out.println("initJAI start");
   initJAI();
   System.out.println("initJAI end");
   //Do something ...
""", "java")}


${h3("Handling the class loader")}

When defining the task on the OpenMOLE script, you have to ensure that all JAI classes are managed by the same class loader by using the dedicated function ${em{"withThreadClassLoader"}}.
When using the Java code as a library, it requires some update on the script as follow (for example, for the TiffReaderTask):

$br$br

${hl.openmole(s"""
val fileIn = Val[File]
val fileOut = Val[File]

val GeoTifWriter =
  ScalaTask($tq
    |import fr.openmole.geotools.tiff._
    |withThreadClassLoader(TiffReaderTask.getClassLoader())(TiffWriterTask.write(fileIn, fileOut))
  $tq.stripMargin) set (
    inputs += fileIn,
    outputs += fileOut,
    libraries += workDirectory / "test-mupcity-openmole-0.0.1-SNAPSHOT-shaded.jar",
    fileIn :=  workDirectory / "data" / "test.tif"
  ) hook CopyFileHook(fileOut, workDirectory / "data" / "out.tif")
""")}

$br

The static method ${em{"getClassLoader()"}}, is a simple code that returns the class loader of the class TiffReaderTask.

""")
