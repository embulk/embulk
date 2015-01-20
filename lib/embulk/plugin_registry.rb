
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
      search(type)
      if value = @map[type]
        return value
      end
      raise ConfigError, "Unknown #{@category} plugin '#{type}'."
    end

    def search(type)
      name = "#{@search_prefix}#{type}"
      begin
        require name
        return
      rescue LoadError
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
          return
        rescue LoadError
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
        end
      end
    end
  end
end
