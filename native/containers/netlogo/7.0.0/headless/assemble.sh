
scala-cli --power package -f --jar "$NETLOGO_HOME/lib/app" --dep "com.thoughtworks.xstream:xstream:1.4.20" -o headless.jar  --java Headless.java --library
scala-cli --power package -f -o init.jar --java Init.java --library
