# Embulk: plugin-based bulk data loader

## What's Embulk?

TODO

## Quick Start

The single-file package is the simplest way to try Embulk. You can download the latest embulk.jar from [releases]() and run it with java:

```
wget https://github.com/embulk/embulk/releases .... /latest
java -jar embulk.jar --help
```

Let's load a CSV file, for example. `embulk bundle` subcommand generates a CSV file for you.

```
java -jar embulk.jar bundle ./data
java -jar embulk.jar guess ./data/examples/csv-stdout.yml -o example.yml
java -jar embulk.jar preview example.yml
java -jar embulk.jar run example.yml
```

### Using plugins

You can use plugins to load data from/to various systems and file formats.
An example is [embulk-output-postgres-json]() plugin. It outputs data into PostgreSQL server using "json" column type.

```
java -jar embulk.jar gem install embulk-output-postgres-json
java -jar embulk.jar gem list
```

You can search plugins on RubyGems: [search for "embulk-"](https://rubygems.org/search?utf8=%E2%9C%93&query=embulk-).

## Plugin environment

TODO

## Embulk Development

### Build

```
rake
java -jar embulk.jar guess ./lib/embulk/data/bundle/examples/csv-stdout.yml > config.yml
java -jar embulk.jar preview config.yml
java -jar embulk.jar run config.yml
```

You can see JaCoCo's test coverage report at ${project}/target/site/jacoco/index.html

