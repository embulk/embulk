## Build

```
mvn clean package dependency:copy-dependencies -Dproject.check.skip-findbugs=true
QUICKLOAD_HOME=. java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.quickload.cli.QuickLoad examples/config.yml
```
