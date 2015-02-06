module Embulk

  require 'embulk/plugin'

  class JavaPlugin
    def self.classloader(dir)
      jars = Dir["#{dir}/**/*.jar"]
      urls = jars.map {|jar| "file://#{File.expand_path(jar)}" }
      classloader = Java::PluginClassLoader.new(JRuby.runtime, urls)
    end

    def self.register_guess(name, jar_dir, class_fqdn)
      java_class = classloader(jar_dir).findClass(class_fqdn)
      Plugin.register_java_guess(name, java_class)
    end

    def self.ruby_adapter_class(java_class_, ruby_base_class, ruby_module)
      Class.new(ruby_base_class) do
        define_singleton_method(:java_class) do
          java_class_
        end

        define_singleton_method(:new_java) do
          Java.injector.getInstance(java_class_)
        end

        define_method(:java_object) do
          @java_object ||= self.class.new_java
        end

        include ruby_module
        extend ruby_module::ClassMethods
      end
    end
  end
end
