require 'bundler'
require 'yard'

desc 'Generate YARD document'
YARD::Rake::YardocTask.new(:doc) do |t|
  t.files   = ['lib/**/*.rb']
  t.options = %w[-M kramdown]
  t.options << '--debug' << '--verbose' if $trace
end

task :help do
  puts "  mvn package             : build jar package (TODO not fully implemented yet)"
  puts "  bundle exec rake doc    : build plugin API document for JRuby to doc"
  puts ""
end

task :default => :help
