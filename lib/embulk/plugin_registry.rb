
module Embulk
  require 'embulk/error'

  class PluginRegistry
    def initialize(category, search_prefix)
      @category = category
      @search_prefix = search_prefix
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
        raise PluginLoadError, "Unknown #{@category} plugin '#{type}'. Succeeded found #{@search_prefix}#{type}.rb from installed gems but it did not correctly register plugin."
      else
        raise PluginLoadError, "Unknown #{@category} plugin '#{type}'. #{@search_prefix}#{type}.rb is not installed. Run 'gem search -rd embulk-#{@category}' command to find the plugin gem."
      end
    end

    def search(type)
      name = "#{@search_prefix}#{type}"
      begin
        require name
        return true
      rescue LoadError => e
        # catch LoadError but don't catch ClassNotFoundException
        # TODO: the best code here is to raise exception only if
        #       `name` file is not in $LOAD_PATH.
        raise e if e.to_s =~ /java.lang.ClassNotFoundException/
      end

      # search from $LOAD_PATH
      load_paths = $LOAD_PATH.map do |lp|
        lpath = File.expand_path(File.join(lp, "#{name}.rb"))
        File.exist?(lpath) ? lpath : nil
      end

      paths = [name] + load_paths.compact.sort  # sort to prefer newer version
      paths.each do |path|
        begin
          require path
          return true
        rescue LoadError => e
          raise e if e.to_s =~ /java.lang.ClassNotFoundException/
        end
      end

      # search gems
      if defined?(::Gem::Specification) && ::Gem::Specification.respond_to?(:find_all)
        specs = Gem::Specification.find_all do |spec|
          spec.contains_requirable_file? name
        end

        # prefer newer version
        specs = specs.sort_by {|spec| spec.version }
        if spec = specs.last
          spec.require_paths.each do |lib|
            require "#{spec.full_gem_path}/#{lib}/#{name}"
          end
          return true
        end
      end

      return false
    end
  end
end
