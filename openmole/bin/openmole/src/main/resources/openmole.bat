set startdir=%cd%
set PWD=%~dp0
cd /d %~dp0
start /MIN dbserver\bin\openmole-dbserver.bat
cd %cd%
mkdir "%UserProfile%\.openmole\.tmp"
set ran="%UserProfile%\.openmole\.tmp\%random%"
java -d64 -version >nul 2>&1
if errorlevel 1 goto is32bit
set FLAG="-XX:+UseCompressedOops"
:is32bit
java -Dfile.encoding=UTF-8 -Dosgi.locking=none -Dopenmole.location="%PWD%\" -Dosgi.classloader.singleThreadLoads=true -Dosgi.configuration.area=%ran% -splash:splashscreen.png -XX:MaxPermSize=128M -XX:+UseG1GC -Xmx1G  -XX:MaxPermSize=128M %FLAG% -jar "%PWD%/plugins/org.eclipse.equinox.launcher.jar" -consoleLog -cp "%PWD%/openmole-plugins" -gp "%PWD%/openmole-plugins-gui" %*
rmdir /s /q %ran%
