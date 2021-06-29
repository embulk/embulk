module Embulk
  require 'json'

  module Impl
    # copied from https://github.com/intridea/hashie/blob/da232547c29673a0d7a79c7bf2670f1ea76813ed/lib/hashie/extensions/indifferent_access.rb
    module IndifferentAccess
      def self.included(base)
        #Hashie::Extensions::Dash::IndifferentAccess::ClassMethods.tap do |extension|
        #  base.extend(extension) if base <= Hashie::Dash && !base.singleton_class.included_modules.include?(extension)
        #end

        base.class_eval do
          alias_method :regular_writer, :[]= unless method_defined?(:regular_writer)
          alias_method :[]=, :indifferent_writer
          alias_method :store, :indifferent_writer
          %w(default update replace fetch delete key? values_at).each do |m|
            alias_method "regular_#{m}", m unless method_defined?("regular_#{m}")
            alias_method m, "indifferent_#{m}"
          end

          %w(include? member? has_key?).each do |key_alias|
            alias_method key_alias, :indifferent_key?
          end

          class << self
            def [](*)
              super.convert!
            end

            def try_convert(*)
              (hash = super) && self[hash]
            end
          end
        end
      end

      def self.inject!(hash)
        (class << hash; self; end).send :include, IndifferentAccess
        hash.convert!
      end

      # Injects indifferent access into a duplicate of the hash
      # provided. See #inject!
      def self.inject(hash)
        inject!(hash.dup)
      end

      def convert_key(key)
        key.to_s
      end

      def convert!
        keys.each do |k|
          regular_writer convert_key(k), indifferent_value(regular_delete(k))
        end
        self
      end

      def indifferent_value(value)
        if hash_lacking_indifference?(value)
          IndifferentAccess.inject!(value)
        elsif value.is_a?(::Array)
          value.replace(value.map { |e| indifferent_value(e) })
        else
          value
        end
      end

      def indifferent_default(key = nil)
        return self[convert_key(key)] if key?(key)
        regular_default(key)
      end

      def indifferent_update(other_hash)
        return regular_update(other_hash) if hash_with_indifference?(other_hash)
        other_hash.each_pair do |k, v|
          self[k] = v
        end
      end

      def indifferent_writer(key, value)
        regular_writer convert_key(key), indifferent_value(value)
      end

      def indifferent_fetch(key, *args, &block)
        regular_fetch convert_key(key), *args, &block
      end

      def indifferent_delete(key)
        regular_delete convert_key(key)
      end

      def indifferent_key?(key)
        regular_key? convert_key(key)
      end

      def indifferent_values_at(*indices)
        indices.map { |i| self[i] }
      end

      def indifferent_access?
        true
      end

      def indifferent_replace(other_hash)
        (keys - other_hash.keys).each { |key| delete(key) }
        other_hash.each { |key, value| self[key] = value }
        self
      end

      protected

      def hash_lacking_indifference?(other)
        other.is_a?(::Hash) &&
          !(other.respond_to?(:indifferent_access?) &&
            other.indifferent_access?)
      end

      def hash_with_indifference?(other)
        other.is_a?(::Hash) &&
          other.respond_to?(:indifferent_access?) &&
          other.indifferent_access?
      end
    end
  end

  class DataSource < Hash
    include Impl::IndifferentAccess

    def initialize(hash={}, default=nil, &block)
      if default.nil?
        super(&block)
      else
        super(default)
      end
      hash.each {|key,value| self[key] = value }
    end

    def param(key, type, options={})
      if self.has_key?(key)
        v = self[key]
        value =
          case type
          when :integer
            begin
              Integer(v)
            rescue => e
              raise ConfigError.new e
            end
          when :float
            begin
              Float(v)
            rescue => e
              raise ConfigError.new e
            end
          when :string
            begin
              String(v).dup
            rescue => e
              raise ConfigError.new e
            end
          when :bool
            begin
              !!v  # TODO validation
            rescue => e
              raise ConfigError.new e
            end
          when :hash
            raise ConfigError.new "Invalid value for :hash" unless v.is_a?(Hash)
            DataSource.new.merge!(v)
          when :array
            raise ConfigError.new "Invalid value for :array" unless v.is_a?(Array)
            v.dup
          else
            unless type.respond_to?(:load)
              raise ArgumentError, "Unknown type #{type.to_s.dump}"
            end
            begin
              type.load(v)
            rescue => e
              raise ConfigError.new e
            end
          end

      elsif options.has_key?(:default)
        value = options[:default]

      else
        raise ConfigError.new "Required field #{key.to_s.dump} is not set"
      end

      return value
    end

    def self.from_java(java_data_source_impl)
      json = java_data_source_impl.toString
      new.merge!(JSON.parse(json))
    end

    def self.from_ruby_hash(hash)
      new.merge!(hash)
    end

    def to_java
      json = to_json
      Java::Injected::ModelManager.readObjectAsDataSource(json.to_java)
    end

    def load_config(task_type)
      Java::Injected::ModelManager.readObjectWithConfigSerDe(task_type.java_class, to_json.to_java)
    end

    def load_task(task_type)
      Java::Injected::ModelManager.readObject(task_type.java_class, to_json.to_java)
    end
  end

end
