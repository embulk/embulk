---
EEP: 
Title: ServiceLoader-style Plugins
Author: hishidama
Status: 
Type: 
Created: 2023-07-02
---

Introduction
=============

Javaのサービスプロバイダーインターフェース（SPI・ https://docs.oracle.com/javase/jp/8/docs/api/java/util/ServiceLoader.html ）を使ってプラグインを読み込む仕組みを試しに作ってみました。

Motivation
===========

サービスプロバイダーインターフェースはJavaで動的にサービスを提供する標準的な方法です。

プラグインのjarファイル（複数可）が配置されたディレクトリーを指定するだけで プラグインが使えるようになります。  
（ユーザーの実行環境のembulk.propertiesに何かを追記したりする必要はありません）

Mavenリポジトリーの構成に従う必要は無く、Gradleだけでプラグインに必要なjarファイルを生成でき、作成したプラグインを試験的に実行する際にjarファイルを指定しやすくなります。  
（余談ですが、世の中にはインターネットに接続できない環境でソフトウェアをインストールしなければいけないという場所もあり、そういう環境でMavenリポジトリー構成を保ったままインストールするのはなかなか大変です）

また、（良いかどうかは別として、）複数のプラグインをひとつのGitリポジトリーでソース管理する（ひとつのjarファイルに複数のプラグインを含める）ことも出来るようになります。  
（例えば、DBのinputとoutputのプラグインをひとつのリポジトリーで管理し、そのjarファイルをインストールするだけで両方のプラグインが使えます）

ServiceLoader-style Plugins
============================

embulk-spi
-----------

embulk-spiに `org.embulk.spi.EmbulkPluginFactory` インターフェースを新設します。  
プラグインの名前やクラスを返すインターフェースです。

プラグインの実装
----------------

プラグイン側では、 `org.embulk.spi.EmbulkPluginFactory` インターフェースを実装したクラスを用意します。

```java
import org.embulk.spi.EmbulkPluginFactory;

public class Example11OutputPluginFactory implements EmbulkPluginFactory {

    @Override
    public String getName() {
        return "example11";
    }

    @Override
    public String getVersion() {
        return "0.1.0-SNAPSHOT";
    }

    @Override
    public Class<?> getPluginClass() {
        return Example11OutputPlugin.class;
    }
}
```

そして、生成されるjarファイルに META-INF/services/org.embulk.spi.EmbulkPluginFactory ファイルを含めるようにします。  
（src/main/resource/META-INF/services/org.embulk.spi.EmbulkPluginFactory を用意し、上記のExample11OutputPluginFactoryクラスを記載します）

実行時に依存ライブラリーも読み込めるようにする為に、プラグイン本体のjarファイルだけでなく、依存ライブラリーのjarファイルもディレクトリーに配置するようにします。  
例えばbuild.gradleに `java-library-distribution` プラグインを追加すると、installDistタスクによって必要なjarファイルが配置できます。

```bash
$ cd $PLUGIN_DEVELOP_DIR
$ ./gradlew installDist
$ ls -R build/install
```

実行例
-------

runコマンドの引数でディレクトリーを指定する例です。  
（-I はJRuby用のディレクトリーを指定するオプションのようですが、今回はこれをそのまま流用しました）

```bash
$ java -jar embulk-0.11.0-SNAPSHOT.jar run config.yml -I $PLUGIN_DEVELOP_DIR/build/install/embulk-output-example11
```

また、embulk_home/pluginsの下に置かれているjarファイルを読み込むようにしてあります。

```bash
$ mv $PLUGIN_DEVELOP_DIR/bulid/install/embulk-output-example11 $EMBULK_HOME/plugins/embulk-output-example11-0.1.0-SNAPSHOT
$ java -jar embulk-0.11.0-SNAPSHOT.jar run config.yml
```

Copyright / License
====================

This document is placed under the [CC0-1.0-Universal](https://creativecommons.org/publicdomain/zero/1.0/deed.en) license, whichever is more permissive.
