module Embulk

  require 'embulk/plugin'

  class JavaPlugin
    def self.classloader(dir)
      jars = Dir["#{dir}/**/*.jar"]
      urls = jars.map {|jar| java.io.File.new(File.expand_path(jar)).toURI.toURL }
      begin
        expected_temporary_variable_name = Java::org.embulk.jruby.JRubyPluginSource::PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME
      rescue => e
        raise PluginLoadError.new "Java's org.embulk.jruby.JRubyPluginSource does not define PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME unexpectedly."
      end
      if expected_temporary_variable_name != "$temporary_internal_plugin_class_loader_factory__"
        raise PluginLoadError.new "Java's org.embulk.jruby.JRubyPluginSource does not define PLUGIN_CLASS_LOADER_FACTORY_VARIABLE_NAME correctly."
      end
      factory = $temporary_internal_plugin_class_loader_factory__
      factory.create(urls, JRuby.runtime.getJRubyClassLoader())
    end

    def self.register_input(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)  # TODO handle class not found error
      Plugin.register_java_input(name, java_class)
    end

    def self.register_output(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_output(name, java_class)
    end

    def self.register_filter(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_filter(name, java_class)
    end

    def self.register_parser(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_parser(name, java_class)
    end

    def self.register_formatter(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_formatter(name, java_class)
    end

    def self.register_decoder(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_decoder(name, java_class)
    end

    def self.register_encoder(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_encoder(name, java_class)
    end

    def self.register_guess(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_guess(name, java_class)
    end

    def self.register_executor(name, class_fqdn, jar_dir)
      java_class = classloader(jar_dir).loadClass(class_fqdn)
      Plugin.register_java_executor(name, java_class)
    end

    def self.ruby_adapter_class(java_class, ruby_base_class, ruby_module)
      Class.new(ruby_base_class) do
        const_set(:PLUGIN_JAVA_CLASS, java_class)

        include ruby_module
        extend ruby_module::ClassMethods

        unless method_defined?(:plugin_java_object)
          def plugin_java_object
            @plugin_java_object ||= self.class.new_java
          end
        end

        unless (class<<self;self;end).method_defined?(:plugin_java_class)
          def self.plugin_java_class
            self::PLUGIN_JAVA_CLASS
          end
        end

        # TODO ruby_base_class already implements new_java. So
        #      this line returns always true:
        #unless (class<<self;self;end).method_defined?(:new_java)
        #      but this line could return false unexpectedly if
        #      ruby_module::ClassMethods includes other modules.
        unless ruby_module::ClassMethods.method_defined?(:new_java)
          def self.new_java
            Java::org.embulk.plugin.PluginManager.newPluginInstance(plugin_java_class, Embulk::Java::EmbulkSystemProperties)
          end
        end
      end
    end
  end
end
