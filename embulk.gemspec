$LOAD_PATH.push File.expand_path("../lib", __FILE__)
require 'embulk/version'

Gem::Specification.new do |gem|
  gem.name          = "embulk"
  gem.version       = Embulk::VERSION

  gem.summary       = "Embulk, a plugin-based parallel bulk data loader"
  gem.description   = "Embulk is an open-source, plugin-based bulk data loader to scale and simplify data management across heterogeneous data stores. It can collect and ship any kinds of data in high throughput with transaction control."
  gem.authors       = ["Sadayuki Furuhashi"]
  gem.email         = ["frsyuki@gmail.com"]
  gem.license       = "Apache 2.0"
  gem.homepage      = "https://github.com/embulk/embulk"

  provided_classpath = Dir["classpath/jruby-complete-*.jar"] + Dir["classpath/icu4j-*.jar"]
  gem.files         = `git ls-files`.split("\n") + Dir["classpath/*.jar"] - provided_classpath
  gem.test_files    = gem.files.grep(%r"^(test|spec)/")
  gem.executables   = gem.files.grep(%r"^bin/").map{ |f| File.basename(f) }
  gem.require_paths = ["lib"]
  gem.has_rdoc      = false

  if RUBY_PLATFORM =~ /java/i
    gem.add_dependency "bundler", '>= 1.10.6'
    gem.add_dependency "msgpack", '~> 0.7.3'
    gem.add_dependency "liquid", '~> 3.0.6'

    # For embulk/guess/charset.rb. See also embulk-core/build.gradle
    gem.add_dependency "rjack-icu", '~> 4.54.1.1'

    gem.platform = 'java'

  else
    gem.add_dependency "jruby-jars", '= 9.1.5.0'
  end

  gem.add_development_dependency "rake", [">= 0.10.0"]
  gem.add_development_dependency "test-unit", ["~> 3.0.9"]
  gem.add_development_dependency "yard", ["~> 0.8.7"]
  gem.add_development_dependency "kramdown", ["~> 1.5.0"]
end
