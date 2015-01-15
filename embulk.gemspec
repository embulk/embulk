$LOAD_PATH.push File.expand_path("../lib", __FILE__)
require 'embulk/version'

Gem::Specification.new do |s|
  s.name = "embulk"
  s.version = Embulk::VERSION
  s.summary = %q{Embulk data sync framework}
  s.description = %q{Embulk is an open-source, plugin-based data synchronizer to scale and simplify data management across heterogeneous storages. It can collect and ship many kinds of data in high throughput.}
  s.author = "Sadayuki Furuhashi"
  s.email = "frsyuki@gmail.com"
  s.license = "Apache 2.0"
  s.homepage = "http://embulk.org/"
  s.rubyforge_project = "embulk"
  s.has_rdoc = false
  s.files = `git ls-files`.split("\n") + Dir["classpath/*.jar"]
  s.test_files = `git ls-files -- {test,spec}/*`.split("\n")
  s.require_paths = ["lib"]

  s.add_development_dependency 'bundler', ['~> 1.0']
  s.add_development_dependency 'rake', ['~> 0.9.2']
  s.add_development_dependency 'rspec', ['~> 2.11']
  s.add_development_dependency 'json', ['~> 1.7']
  s.add_development_dependency 'yard', ['~> 0.8.7']
  s.add_development_dependency 'kramdown', ['~> 1.5.0']
end
