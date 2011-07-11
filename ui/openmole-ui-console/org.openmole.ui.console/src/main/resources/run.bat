rmdir /s /q "configuration\org.eclipse.core.runtime"
rmdir /s /q "configuration\org.eclipse.equinox.app"
rmdir /s /q "configuration\org.eclipse.osgi"
java -ea -Dosgi.classloader.singleThreadLoads=true -Xmx512m -jar plugins\org.eclipse.equinox.launcher.jar -p openmole-plugins,openmole-plugins-ui %*
