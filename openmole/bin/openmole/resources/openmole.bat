rmdir /s /q "configuration\org.eclipse.core.runtime"
rmdir /s /q "configuration\org.eclipse.equinox.app"
rmdir /s /q "configuration\org.eclipse.osgi"

start dbserver\bin\openmole-dbserver.bat

mkdir "%UserProfile%\.openmole\.tmp"

set ran="%UserProfile%\.openmole\.tmp\%random%"

set PWD=%~dp0

java -d64 -version >nul 2>&1
if errorlevel 1 goto is32bit
set FLAG="-XX:+UseCompressedOops"
:is32bit

java -Dosgi.locking=none -Dopenmole.location="%PWD%\" -Dosgi.classloader.singleThreadLoads=true -Dosgi.configuration.area=%ran% -splash:splashscreen.png -XX:MaxPermSize=128M -XX:+UseG1GC -Xmx1G  -XX:MaxPermSize=128M %FLAG% -jar ./plugins/org.eclipse.equinox.launcher.jar -cp ./openmole-plugins -gp ./openmole-plugins-gui %*

rmdir /s /q %ran%
