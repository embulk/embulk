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

  gem.files         = `git ls-files`.split("\n") + Dir["classpath/*.jar"]
  gem.test_files    = gem.files.grep(%r"^(test|spec)/")
  gem.executables   = gem.files.grep(%r"^bin/").map{ |f| File.basename(f) }
  gem.require_paths = ["lib"]
  gem.has_rdoc      = false

  gem.add_development_dependency "bundler", [">= 1.0"]
  gem.add_development_dependency "rake", [">= 0.10.0"]
  gem.add_development_dependency "test-unit", ["~> 3.0.9"]
  gem.add_development_dependency "yard", ["~> 0.8.7"]
  gem.add_development_dependency "kramdown", ["~> 1.5.0"]
end
