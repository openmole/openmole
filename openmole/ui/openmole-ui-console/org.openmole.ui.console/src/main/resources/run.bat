FOR /F "tokens=*" %%G IN ('DIR /B /AD /S org.*') DO RMDIR /S /Q %%G
java -Xmx512m -jar plugins/org.eclipse.equinox.launcher_1.1.0.jar -p openmole-plugins,openmole-plugins-ui -vmargs -Dosgi.framework.extensions=org.eclipse.equinox.weaving.hook
