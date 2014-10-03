## Build

```
mvn clean package dependency:copy-dependencies -Dproject.check.skip-findbugs=true
java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.quickload.cli.QuickLoad
```
