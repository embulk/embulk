# Embulk

A plugin-based parallel bulk data loader that makes painful data integration works relaxed.

## What's Embulk?

Embulk is a plugin-based parallel bulk data loader that helps **data transfer** between various **storages**, **databases**, **NoSQL** and **cloud services**.

You can install input and output plugins to integrate many other file formats and storages.

You also can release plugins to share your efforts of data cleaning, error handling, transaction control, and retrying. 
Packaging effrots into plugins **brings OSS-style development to the data scripts** which **was tend to be one-time adhoc scripts**.

[Embuk, an open-source plugin-based parallel bulk data loader](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed) at Slideshare

[![Embulk](https://gist.githubusercontent.com/frsyuki/f322a77ee2766a508ba9/raw/e8539b6b4fda1b3357e8c79d3966aa8148dbdbd3/embulk-overview.png)](http://www.slideshare.net/frsyuki/embuk-making-data-integration-works-relaxed/12)


## Quick Start

The single-file package is the simplest way to try Embulk. You can download the latest embulk-VERSION.jar from [the releases page](https://bintray.com/embulk/maven/embulk/view#files) and run it with java:

```
wget https://bintray.com/artifact/download/embulk/maven/embulk-0.2.0.jar -O embulk.jar
java -jar embulk.jar --help
```

Let's load a CSV file, for example. `embulk example` subcommand generates a csv file and config file for you.

```
java -jar embulk.jar example ./try1
java -jar embulk.jar guess   ./try1/example.yml -o config.yml
java -jar embulk.jar preview config.yml
java -jar embulk.jar run     config.yml
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

`embulk bundle` subcommand creates (or updates if already exists) a *plugin bundle* directory.
You can use the bundle using `-b <bundle_dir>` option. `embulk bundle` also generates some example plugins to \<bundle_dir>/embulk/\*.rb directory.

See generated \<bundle_dir>/Gemfile file how to plugin bundles work.

```
java -jar embulk.jar bundle ./embulk_bundle
java -jar embulk.jar guess  -b ./embulk_bundle ...
java -jar embulk.jar run    -b ./embulk_bundle ...
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

### Release

You need to add your bintray account information to ~/.gradle/gradle.properties

```
bintray_user=(bintray user name)
bintray_api_key=(bintray api key)
```

Increment version number written at following 3 files (TODO improve this):

* build.gradle
* pom.xml
* lib/embulk/version.rb

Then, build and upload using gradle:

```
./gradlew bintrayUpload
```

Finally, you need to manually upload the single-file jar package to bintray.
Run `rake` and upload embulk-VERSION.jar from "Upload Files" link at https://bintray.com/embulk/maven/embulk/VERSION/upload.

```
rake
# embulk-VERSION.jar is built
```

See also:
* [Bintray](https://bintray.com)
* [How to acquire bintray API Keys](https://bintray.com/docs/usermanual/interacting/interacting_apikeys.html)

