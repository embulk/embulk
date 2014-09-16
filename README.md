## Build

```
mvn clean package dependency:copy-dependencies
java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.quickload.cli.QuickLoad
```
