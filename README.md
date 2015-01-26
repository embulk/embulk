# Embulk: plugin-based parallel bulk data loader

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
java -jar embulk.jar bundle ./mydata
java -jar embulk.jar guess  ./mydata/examples/csv-stdout.yml -o example.yml
java -jar embulk.jar preview example.yml
java -jar embulk.jar run     example.yml
```

### Using plugins

You can use plugins to load data from/to various systems and file formats.
An example is [embulk-output-postgres-json]() plugin. It outputs data into PostgreSQL server using "json" column type.

```
java -jar embulk.jar gem install embulk-output-postgres-json
java -jar embulk.jar gem list
```

You can search plugins on RubyGems: [search for "embulk-"](https://rubygems.org/search?utf8=%E2%9C%93&query=embulk-).

### Using plugin bundle

`embulk bundle` subcommand creates (or updates if already exists) a bundle directory.
You can use the bundle using `-b <bundle_dir>` option

`embulk bundle` also generates some example plugins to \<bundle_dir>/embulk/\*.rb directory.

See generated \<bundle_dir>/Gemfile file how to plugin bundles work.

```
sed -i .orig s/stdout/example/ ./mydata/examples/csv-stdout.yml
java -jar embulk.jar guess  -b ./mydata ./mydata/examples/csv-stdout.yml -o example.yml
java -jar embulk.jar run    -b ./mydata example.yml
```

### Releasing plugins to RubyGems

TODO: documents

```
embulk-plugin-xyz
```

## Embulk Development

### Build

```
rake  # creates embulk-VERSION.jar
java -jar embulk-0.x.y.jar guess ./lib/embulk/data/bundle/examples/csv-stdout.yml > config.yml
java -jar embulk-0.x.y.jar preview config.yml
java -jar embulk-0.x.y.jar run config.yml
```

You can see JaCoCo's test coverage report at ${project}/target/site/jacoco/index.html

