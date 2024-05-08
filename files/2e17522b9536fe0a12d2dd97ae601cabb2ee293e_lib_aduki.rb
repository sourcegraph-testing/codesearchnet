require "date"
require "time"
require "aduki/version"
require "aduki/recursive_hash"
require "aduki/attr_finder"

module Aduki
  def self.install_monkey_patches
    require 'core_ext/array'
    require 'core_ext/hash'
  end

  def self.to_aduki obj, collector={ }, key="", join=""
    case obj
    when Hash
      obj.keys.inject(collector) { |result, k|
        v = obj[k]
        to_aduki v, collector, "#{key}#{join}#{k}", "."
        result
      }
    when Array
      obj.each_with_index do |av, ix|
        to_aduki av, collector, "#{key}[#{ix}]", "."
      end
    when String, Numeric, Symbol
      collector[key] = obj
    else
      vv = obj.instance_variables
      vv.each do |v|
        accessor = v.to_s.gsub(/^@/, "").to_sym
        if obj.respond_to?(accessor) && obj.respond_to?("#{accessor}=".to_sym)
          to_aduki obj.send(accessor), collector, "#{key}#{join}#{accessor}", "."
        end
      end
    end
    collector
  end

  def self.maybe_parse_date str
    return nil if (str == nil) || (str == '')
    Date.parse(str)
  end

  def self.to_value klass, setter, value
    return value.map { |v| to_value klass, setter, v} if value.is_a? Array

    type = klass.aduki_type_for_attribute_name setter
    if type && type.is_a?(Class) && (value.class <= type)
      value
    elsif type.is_a? Hash
      to_typed_hash type.values.first, value
    elsif type == Date
      case value
      when Date; value
      when String; maybe_parse_date(value)
      else
        if value.respond_to?(:to_date)
          value.to_date
        end
      end
    elsif type == Time
      case value
      when Time; value
      when String; Time.parse(value)
      else
        if value.respond_to?(:to_time)
          value.to_time
        end
      end
    elsif type && (type <= Integer)
      value.to_i
    elsif type && (type <= Float)
      value.to_f
    elsif type.respond_to? :aduki_find
      type.aduki_find value
    else
      type ? type.new(value) : value
    end
  end

  def self.to_typed_hash klass, value
    setters = split_attributes value
    hsh = { }
    setters.each { |k, v|
      hsh[k] = klass.new(v)
    }
    hsh
  end

  def self.split_attributes attrs
    setters = { }
    attrs.each do |setter, value|
      if setter.match(/\./)
        first, rest = setter.split(/\./, 2)
        setters[first] ||= { }
        setters[first][rest] = value
      else
        setters[setter.to_s] = value
      end
    end
    setters
  end

  def self.apply_array_attribute klass, object, getter, value
    setter_method = "#{getter}=".to_sym
    return unless object.respond_to? setter_method
    array = object.send(getter) || []
    array << to_value(klass, getter, value)
    object.send(setter_method, array)
  end

  def self.apply_new_single_attribute klass, object, setter, value
    setter_method = "#{setter}=".to_sym
    return unless object.respond_to? setter_method

    object.send setter_method, to_value(klass, setter, value)
  end

  def self.apply_single_attribute klass, object, setter, value
    if value.is_a?(Hash)
      existing_value = object.send setter if object.respond_to?(setter)
      if existing_value
        if existing_value.is_a? Hash
          value.each { |k, v| existing_value[k] = v }
        else
          Aduki.apply_attributes existing_value, value
        end
        return
      end
    end
    apply_new_single_attribute klass, object, setter, value
  end

  def self.apply_attribute klass, object, setter, value
    if setter.match(/\[\d+\]/)
      getter = setter.gsub(/\[\d+\]/, '').to_sym
      apply_array_attribute klass, object, getter, value
    else
      apply_single_attribute klass, object, setter, value
    end
  end

  def self.apply_attributes object, attrs
    attrs = nil if attrs == ''
    setters = split_attributes(attrs || { })
    klass = object.class

    setters.sort.each do |setter, value|
      apply_attribute klass, object, setter, value
    end
  end

  module ClassMethods
    @@types = { }
    @@initializers = { }

    def aduki types
      @@types[self] ||= { }
      @@types[self] = @@types[self].merge types
      types.each do |attr, k|
        attr = attr.to_sym
        attr_reader attr unless method_defined? attr
        attr_writer attr unless method_defined? :"#{attr}="
      end
    end

    def aduki_initialize name, initial_klass, type=:notset
      type = (type == :notset) ? initial_klass : type
      aduki(name => type) if type
      initializer_name = :"aduki_initialize_#{name}"
      define_method initializer_name do
        send :"#{name}=", initial_klass.new
      end
      @@initializers[self] ||= []
      @@initializers[self] << initializer_name
    end

    def aduki_type_for_attribute_name name
      hsh = @@types[self]
      hsh ? hsh[name.to_sym] : nil
    end

    def get_aduki_initializers
      @@initializers[self] || []
    end

    def attr_finder finder, id, *args
      class_eval Aduki::AttrFinder.attr_finders_text(finder, id, *args)
    end

    def attr_many_finder finder, id, name, options={ }
      class_eval Aduki::AttrFinder.one2many_attr_finder_text(finder, id, name, options)
    end
  end

  module Initializer
    def initialize attrs={ }
      self.class.get_aduki_initializers.each { |initializer| send initializer }
      aduki_apply_attributes attrs
      aduki_after_initialize
    end

    def aduki_apply_attributes attrs ; Aduki.apply_attributes self, attrs ; end
    def aduki_after_initialize ; end

    def self.included(base)
      base.extend Aduki::ClassMethods
    end
  end

  # inherit from this class as shortcut instead of including Initializer
  class Initializable ; include Initializer ; end
end
