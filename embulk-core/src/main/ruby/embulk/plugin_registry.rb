
module Embulk
  require 'embulk/error'
  require 'embulk/logger'

  class PluginRegistry
    def initialize(category, search_prefix)
      @category = category
      @search_prefix = search_prefix
      @loaded_gems = {}
      @map = {}
    end

    attr_reader :category

    def register(type, value)
      type = type.to_sym
      @map[type] = value
    end

    def lookup(type)
      type = type.to_sym
      if value = @map[type]
        return value
      end
      if search(type)
        if value = @map[type]
          return value
        end
        raise PluginLoadError.new "Unknown #{@category} plugin '#{type}'. #{@search_prefix}#{type}.rb is installed but it does not correctly register plugin."
      else
        raise PluginLoadError.new "Unknown #{@category} plugin '#{type}'. #{@search_prefix}#{type}.rb is not installed. Run 'embulk gem search -rd embulk-#{@category}' command to find plugins."
      end
    end

    def search(type)
      name = "#{@search_prefix}#{type}"
      begin
        require_and_show name
        return true
      rescue LoadError => e
        # catch LoadError but don't catch ClassNotFoundException
        raise e if e.to_s =~ /java.lang.ClassNotFoundException/
        raise e if $LOAD_PATH.any? {|dir| File.exists? File.join(dir, "#{name}.rb") }
      end

      # search from $LOAD_PATH
      load_path_files = $LOAD_PATH.map do |lp|
        lpath = File.expand_path(File.join(lp, "#{name}.rb"))
        File.exist?(lpath) ? lpath : nil
      end

      paths = load_path_files.compact.sort  # sort to prefer newer version
      paths.each do |path|
        require_and_show path
        return true
      end

      # search gems
      if defined?(::Gem::Specification) && ::Gem::Specification.respond_to?(:find_all)
        specs = Kernel::RUBYGEMS_ACTIVATION_MONITOR.synchronize do  # this lock is added as a workaround of https://github.com/jruby/jruby/issues/3652
          Gem::Specification.find_all do |spec|
            spec.contains_requirable_file? name
          end
        end

        # prefer newer version
        specs = specs.sort_by {|spec| spec.version }
        if spec = specs.last
          spec.require_paths.each do |lib|
            require_and_show "#{spec.full_gem_path}/#{lib}/#{name}", spec
          end
          return true
        end
      end

      return false
    end

    def require_and_show(path, spec=nil)
      require path
      unless spec
        name, spec = Kernel::RUBYGEMS_ACTIVATION_MONITOR.synchronize do  # this lock is added as a workaround of https://github.com/jruby/jruby/issues/3652
          Gem.loaded_specs.find {|name,spec|
            #spec.files.include?(path)
            spec.contains_requirable_file?(path)
          }
        end
      end
      if spec
        unless @loaded_gems[spec.name]
          Embulk.logger.info "Loaded plugin #{spec.name} (#{spec.version})"
          @loaded_gems[spec.name]
        end
      else
        Embulk.logger.info "Loaded plugin #{path} from a load path"
      end
    end
  end
end
