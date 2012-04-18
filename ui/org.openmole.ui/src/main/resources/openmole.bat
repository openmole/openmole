rmdir /s /q "configuration\org.eclipse.core.runtime"
rmdir /s /q "configuration\org.eclipse.equinox.app"
rmdir /s /q "configuration\org.eclipse.osgi"

java -ea -Dosgi.classloader.singleThreadLoads=true -XX:MaxPermSize=128M -XX:+UseParallelGC -Xmx1G -jar plugins\org.eclipse.equinox.launcher.jar -p openmole-plugins -g openmole-plugins-gui %*
