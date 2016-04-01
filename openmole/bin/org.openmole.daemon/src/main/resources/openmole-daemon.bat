set startdir=%cd%
set PWD=%~dp0
if "%PWD:~-1%"=="\" set PWD=%PWD:~0,-1%
cd /d %~dp0

mkdir "%UserProfile%\.openmole\.tmp"
set ran="%UserProfile%\.openmole\.tmp\%random%"

java -ea -Dfile.encoding=UTF-8 -XX:+CMSClassUnloadingEnabled -XX:+UseParallelGC -Xmx92m -cp "%PWD%/launcher/*" org.openmole.launcher.Launcher  --plugins %PWD%/plugins/ --run org.openmole.daemon.Daemon --osgi-directory %ran% --  %*
rmdir /s /q %ran%