
scala-cli run -J -Dfile.encoding=UTF-8 -J -Dnetlogo.extensions.dir="$NETLOGO_HOME/extensions/" -J --add-exports=java.base/java.lang=ALL-UNNAMED --jar "$NETLOGO_HOME/lib/app"  --java Headless.java Test.java --main-class Test
