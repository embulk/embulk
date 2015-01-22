# Embulk: plugin-based bulk data loader

## Quick Start

You can install Embulk using [RubyGems](https://rubygems.org/). `embulk bundle`

```
gem install embulk
embulk bundle  data
embulk guess   -b data data/examples/csv.yml -o config.yml
embulk preview -b data config.yml
embulk run     -b data config.yml
```

## Development

### Packaging

```
rake
```

### Build

```
mvn clean package dependency:copy-dependencies && mv -f embulk-cli/target/dependency/*.jar classpath/
./bin/embulk guess examples/config.yml > config.yml
./bin/embulk preview config.yml
./bin/embulk run config.yml
```

You can see JaCoCo's test coverage report at ${project}/target/site/jacoco/index.html

To build by Gradle, run:
```
./gradlew build
```
If you want to deploy artifacts on local maven repository like ~/.m2/repository/, run:
```
./gradlew install
```
If you want to compile the source code of embulk-core project only, run:
```
./gradlew :embulk-core:compileJava
```
The following command allows use to see the dependency tree of embulk-core project
```
./gradlew :embulk-core:dependencies
```
