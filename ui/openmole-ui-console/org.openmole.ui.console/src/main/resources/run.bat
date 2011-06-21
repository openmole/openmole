FOR /F "tokens=*" %G IN ('DIR /B /AD /S org.*') DO RMDIR /S /Q "%G"
java -ea -Dosgi.classloader.singleThreadLoads=true -Xmx512m -jar plugins\org.eclipse.equinox.launcher.jar -p openmole-plugins,openmole-plugins-ui %*
