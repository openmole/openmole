Annotation based options parsing via Apache Karaf
=================================================

[Apache Karaf](http://karaf.apache.org/) has a nice little shell and command line parsing framework which uses 
annotations on fields/properties to perform type safe injection into Java & Scala objects to provide command line parsing, help and
a full tab-completion interactive shell with nice colour & help.

Since using Scopt, I've moved various projects to using it instead and I highly recommend it (particularly as it makes it easy to put
all your 'commands' into an interactive shell with full tab completion).

For example [Scalate](http://scalate.fusesource.org/) now uses it for all its commands; and to provide a full command line shell. 

So you can invoke individual classes from a JVM as a single command; or invoke the shell and a sub command (like 'git foo') or have a full interactive shell all from essentially the same simple model of writing a class with annotations for options & arguments.

There's not heaps of documentation yet but there are docs on [creating a new shell](http://karaf.apache.org/manual/2.1.99-SNAPSHOT/developers-guide/extending-console.html). 

Bear in mind you can use the command line parsing & interactive shell without any OSGi stuff.

Using Karaf command line parsing and shell
------------------------------------------

Add the following to your pom.xml

    <dependency>
      <groupId>org.apache.karaf.shell</groupId>
      <artifactId>org.apache.karaf.shell.console</artifactId>
      <version>${karaf-version}</version>
    </dependency>


Then add the annotations to your class. For example [here is a sample Run command from Scalate](https://github.com/scalate/scalate/blob/master/scalate-tool/src/main/scala/org/fusesource/scalate/tool/commands/Run.scala#L33). 

Notice we can use the argument annotations to specify the positional arguments along with optoins (which take a prefix) and specify things like whether they are mandatory or their name and description etc.

If using Scala there is a clash between the gogo 'Option' annotation and the scala.Option class, so I tend to add [this line|https://github.com/scalate/scalate/blob/master/scalate-tool/src/main/scala/org/fusesource/scalate/tool/commands/Run.scala#L24] in Scala to alias them

    import org.apache.felix.gogo.commands.{Action, Option => option, Argument => argument, Command => command}

Then [here is how to create a full shell for your commands](https://github.com/scalate/scalate/blob/master/scalate-tool/src/main/scala/org/fusesource/scalate/tool/ScalateMain.scala) along with this [commands.index file](https://github.com/scalate/scalate/blob/master/scalate-tool/src/main/filtered-resources/META-INF/services/org.fusesource.scalate/commands.index)