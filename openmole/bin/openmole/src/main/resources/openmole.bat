set startdir=%cd%
set PWD=%~dp0
if "%PWD:~-1%"=="\" set PWD=%PWD:~0,-1%
cd /d %~dp0
start /MIN dbserver\bin\openmole-dbserver.bat
cd %cd%
mkdir "%UserProfile%\.openmole\.tmp"
set ran="%UserProfile%\.openmole\.tmp\%random%"
java -d64 -version >nul 2>&1
if errorlevel 1 goto is32bit
set FLAG="-XX:+UseCompressedOops"
:is32bit

:run
java -Dlogback.configurationFile="%PWD%/configuration/logback.xml" -Xss1M -Dfile.encoding=UTF-8 -Dosgi.locking=none -Dopenmole.location="%PWD%" -Dosgi.classloader.singleThreadLoads=true -Dosgi.configuration.area=%ran% -XX:MaxPermSize=128M -XX:+UseG1GC -Xmx1G  -XX:MaxPermSize=128M %FLAG% -cp "%PWD%/launcher/*" org.openmole.launcher.Launcher  --plugins %PWD%/plugins/ --run org.openmole.ui.Application --osgi-directory %ran% -- %*
set ret=%errorlevel%
rmdir /s /q %ran%

if %ret% EQU 254 goto run

EXIT /B %ret%
