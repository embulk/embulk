## Build

```
mvn clean package dependency:copy-dependencies
java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.embulk.cli.Embulk examples/config.yml
```

You can see JaCoCo's coverage report at ${project}/target/site/jacoco/index.html

To send coverage report to Coveralls; (coveralls.repo.token may change)

```
mvn clean package coveralls:report -Dcoveralls.repo.token=05RZCkCRpJWXa5vqpUSBSrke3FUDPGkO2 -Dproject.check.skip-findbugs=true
```

## Execution

```
EMBULK_HOME=$(pwd) java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.embulk.cli.Runner guess examples/config.yml > config.yml
EMBULK_HOME=$(pwd) java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.embulk.cli.Runner preview config.yml
EMBULK_HOME=$(pwd) java -cp $(echo $(find */target -name "*.jar") | sed "s/ /:/g") org.embulk.cli.Runner run config.yml
```

