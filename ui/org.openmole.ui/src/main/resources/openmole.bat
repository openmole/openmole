rmdir /s /q "configuration\org.eclipse.core.runtime"
rmdir /s /q "configuration\org.eclipse.equinox.app"
rmdir /s /q "configuration\org.eclipse.osgi"

start dbserver\bin\openmole-dbserver.bat

java -Dosgi.classloader.singleThreadLoads=true -splash:splashscreen.png -XX:MaxPermSize=128M -XX:+UseParallelGC -Xmx1G  -XX:MaxPermSize=128M -XX:+UseParallelGC -jar ./plugins/org.eclipse.equinox.launcher.jar -cp ./openmole-plugins -gp ./openmole-plugins-gui %*

