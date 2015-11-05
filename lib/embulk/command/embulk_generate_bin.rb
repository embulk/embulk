module Embulk
  def self.generate_bin(options={})
    jruby_jar_path = org.jruby.Main.java_class.protection_domain.code_source.location.to_s
    if __FILE__ =~ /^(?:classpath|uri:classloader):/ || __FILE__.include?('!/')
      resource_class = org.embulk.command.Runner.java_class
      ruby_script_path = resource_class.resource("/embulk/command/embulk.rb").to_s
    else
      ruby_script_path = File.join(File.dirname(__FILE__), 'embulk.rb')
    end
    java_home = java.lang.System.properties['java.home']

    generate_bin_data(jruby_jar_path, ruby_script_path, options.merge(java_home: java_home))
  end

  def self.generate_bin_data(jruby_jar_path, ruby_script_path, options={})
    if java_home = options[:java_home]
      java_home_script = %{export JAVA_HOME='#{java_home}'}
      java_path = %{"$JAVA_HOME"/bin/java}
    else
      java_home_script = %{}
      java_path = %{java}
    end

    # TODO parse -D options to set them to java
    shell_script = <<EOF
#!/bin/sh
=begin 2>/dev/null
#{java_home_script}
exec #{java_path} -classpath "$0" org.jruby.Main "$0" "$@"
exit 127
=end
EOF

    if options[:bundle_path] == :here
      bundle_path_script = %{ENV['EMBULK_BUNDLE_PATH'] = File.expand_path('..', File.dirname(__FILE__))}
    elsif path = options[:bundle_path]
      bundle_path_script = %{ENV['EMBULK_BUNDLE_PATH'] = '#{path}'}
    else
      bundle_path_script = b ''
    end

    ruby_init_script = b <<EOF
#{bundle_path_script}
ENV.delete 'GEM_HOME'
ENV.delete 'GEM_PATH'
EOF

    ruby_script = b(File.read(ruby_script_path))
    if i = ruby_script.index(b("\n__END__\n"))
      # delete contents after __END__
      ruby_script = ruby_script[0, i]
    end

    jruby_jar = b(File.read(jruby_jar_path))

    data = shell_script << ruby_init_script + ruby_script << b("\n__END__\n") << jruby_jar
  end

  def self.b(s)
    s.force_encoding('ASCII-8BIT')
  end
end
