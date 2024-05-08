require 'yard'

module Solargraph
  # The YardMap provides access to YARD documentation for the Ruby core, the
  # stdlib, and gems.
  #
  class YardMap
    autoload :Cache,    'solargraph/yard_map/cache'
    autoload :CoreDocs, 'solargraph/yard_map/core_docs'
    autoload :CoreGen,  'solargraph/yard_map/core_gen'

    CoreDocs.require_minimum
    @@stdlib_yardoc = CoreDocs.yardoc_stdlib_file
    @@stdlib_paths = {}
    YARD::Registry.load! @@stdlib_yardoc
    YARD::Registry.all(:class, :module).each do |ns|
      next if ns.file.nil?
      path = ns.file.sub(/^(ext|lib)\//, '').sub(/\.(rb|c)$/, '')
      next if path.start_with?('-')
      @@stdlib_paths[path] ||= []
      @@stdlib_paths[path].push ns
    end

    # @return [Array<String>]
    attr_reader :required

    attr_writer :with_dependencies

    # @param required [Array<String>]
    # @param with_dependencies [Boolean]
    def initialize(required: [], with_dependencies: true)
      # HACK: YardMap needs its own copy of this array
      @required = required.clone
      @with_dependencies = with_dependencies
      @gem_paths = {}
      @stdlib_namespaces = []
      process_requires
      yardocs.uniq!
    end

    # @return [Array<Solargraph::Pin::Base>]
    def pins
      @pins ||= []
    end

    def with_dependencies?
      @with_dependencies ||= true unless @with_dependencies == false
      @with_dependencies
    end

    # @param new_requires [Array<String>]
    # @return [Boolean]
    def change new_requires
      if new_requires.uniq.sort == required.uniq.sort
        false
      else
        required.clear
        required.concat new_requires
        process_requires
        true
      end
    end

    # @return [Array<String>]
    def yardocs
      @yardocs ||= []
    end

    # @return [Array<String>]
    def unresolved_requires
      @unresolved_requires ||= []
    end

    # @param y [String]
    # @return [YARD::Registry]
    def load_yardoc y
      if y.kind_of?(Array)
        YARD::Registry.load y, true
      else
        YARD::Registry.load! y
      end
    rescue Exception => e
      Solargraph::Logging.logger.warn "Error loading yardoc '#{y}' #{e.class} #{e.message}"
      yardocs.delete y
      nil
    end

    # @return [Array<Solargraph::Pin::Base>]
    def core_pins
      @@core_pins ||= begin
        result = []
        load_yardoc CoreDocs.yardoc_file
        YARD::Registry.each do |o|
          result.concat generate_pins(o)
        end
        result
      end
    end

    # @param path [String]
    # @return [Pin::Base]
    def path_pin path
      pins.select{ |p| p.path == path }.first
    end

    private

    # @return [YardMap::Cache]
    def cache
      @cache ||= YardMap::Cache.new
    end

    # @param ns [YARD::CodeObjects::Namespace]
    # @return [Array<Solargraph::Pin::Base>]
    def recurse_namespace_object ns
      result = []
      ns.children.each do |c|
        result.concat generate_pins(c)
        result.concat recurse_namespace_object(c) if c.respond_to?(:children)
      end
      result
    end

    # @param code_object [YARD::CodeObjects::Base]
    # @return [Solargraph::Pin::Base]
    def generate_pins code_object, spec = nil
      result = []
      location = object_location(code_object, spec)
      if code_object.is_a?(YARD::CodeObjects::NamespaceObject)
        result.push Solargraph::Pin::YardPin::Namespace.new(code_object, location)
        if code_object.is_a?(YARD::CodeObjects::ClassObject) and !code_object.superclass.nil?
          # @todo This method of superclass detection is a bit of a hack. If
          #   the superclass is a Proxy, it is assumed to be undefined in its
          #   yardoc and converted to a fully qualified namespace.
          if code_object.superclass.is_a?(YARD::CodeObjects::Proxy)
            superclass = "::#{code_object.superclass}"
          else
            superclass = code_object.superclass.to_s
          end
          result.push Solargraph::Pin::Reference::Superclass.new(location, code_object.path, superclass)
        end
        code_object.class_mixins.each do |m|
          result.push Solargraph::Pin::Reference::Extend.new(location, code_object.path, m.path)
        end
        code_object.instance_mixins.each do |m|
          result.push Solargraph::Pin::Reference::Include.new(location, code_object.path, m.path)
        end
      elsif code_object.is_a?(YARD::CodeObjects::MethodObject)
        if code_object.name == :initialize && code_object.scope == :instance
          # @todo Check the visibility of <Class>.new
          result.push Solargraph::Pin::YardPin::Method.new(code_object, location, 'new', :class, :public)
          result.push Solargraph::Pin::YardPin::Method.new(code_object, location, 'initialize', :instance, :private)
        else
          result.push Solargraph::Pin::YardPin::Method.new(code_object, location)
        end
      elsif code_object.is_a?(YARD::CodeObjects::ConstantObject)
        result.push Solargraph::Pin::YardPin::Constant.new(code_object, location)
      end
      result
    end

    # @return [void]
    def process_requires
      pins.clear
      unresolved_requires.clear
      stdnames = {}
      done = []
      required.each do |r|
        next if r.nil? || r.empty? || done.include?(r)
        done.push r
        cached = cache.get_path_pins(r)
        unless cached.nil?
          pins.concat cached
          next
        end
        result = []
        begin
          spec = Gem::Specification.find_by_path(r) || Gem::Specification.find_by_name(r.split('/').first)
          ver = spec.version.to_s
          ver = ">= 0" if ver.empty?
          yd = YARD::Registry.yardoc_file_for_gem(spec.name, ver)
          # YARD detects gems for certain libraries that do not have a yardoc
          # but exist in the stdlib. `fileutils` is an example. Treat those
          # cases as errors and check the stdlib yardoc.
          raise Gem::LoadError if yd.nil?
          @gem_paths[spec.name] = spec.full_gem_path
          unless yardocs.include?(yd)
            yardocs.unshift yd
            result.concat process_yardoc yd, spec
            result.concat add_gem_dependencies(spec) if with_dependencies?
          end
        rescue Gem::LoadError => e
          stdtmp = []
          @@stdlib_paths.each_pair do |path, objects|
            stdtmp.concat objects if path == r || path.start_with?("#{r}/")
          end
          if stdtmp.empty?
            unresolved_requires.push r
          else
            stdnames[r] = stdtmp
          end
        end
        result.delete_if(&:nil?)
        unless result.empty?
          cache.set_path_pins r, result
          pins.concat result
        end
      end
      pins.concat process_stdlib(stdnames)
      pins.concat core_pins
    end

    # @param required_namespaces [Array<YARD::CodeObjects::Namespace>]
    # @return [Array<Solargraph::Pin::Base>]
    def process_stdlib required_namespaces
      pins = []
      unless required_namespaces.empty?
        yard = load_yardoc @@stdlib_yardoc
        done = []
        required_namespaces.each_pair do |r, objects|
          result = []
          objects.each do |ns|
            next if done.include?(ns.path)
            done.push ns.path
            result.concat generate_pins(ns)
            result.concat recurse_namespace_object(ns)
          end
          result.delete_if(&:nil?)
          cache.set_path_pins(r, result) unless result.empty?
          pins.concat result
        end
      end
      pins
    end

    # @param spec [Gem::Specification]
    # @return [void]
    def add_gem_dependencies spec
      result = []
      (spec.dependencies - spec.development_dependencies).each do |dep|
        begin
          depspec = Gem::Specification.find_by_name(dep.name)
          next if depspec.nil? || @gem_paths.key?(depspec.name)
          @gem_paths[depspec.name] = depspec.full_gem_path
          gy = YARD::Registry.yardoc_file_for_gem(dep.name)
          if gy.nil?
            unresolved_requires.push dep.name
          else
            next if yardocs.include?(gy)
            yardocs.unshift gy
            result.concat process_yardoc gy, depspec
            result.concat add_gem_dependencies(depspec)
          end
        rescue Gem::LoadError
          # This error probably indicates a bug in an installed gem
          Solargraph::Logging.logger.warn "Failed to resolve #{dep.name} gem dependency for #{spec.name}"
        end
      end
      result
    end

    # @param y [String, nil]
    # @return [Array<Pin::Base>]
    def process_yardoc y, spec = nil
      return [] if y.nil?
      size = Dir.glob(File.join(y, '**', '*'))
        .map{ |f| File.size(f) }
        .inject(:+)
      if !size.nil? && size > 20_000_000
        Solargraph::Logging.logger.warn "Yardoc at #{y} is too large to process (#{size} bytes)"
        return []
      end
      result = []
      load_yardoc y
      YARD::Registry.each do |o|
        result.concat generate_pins(o, spec)
      end
      result
    end

    # @param obj [YARD::CodeObjects::Base]
    # @return [Solargraph::Location]
    def object_location obj, spec = nil
      @object_file_cache ||= {}
      return nil if spec.nil? || obj.file.nil? || obj.line.nil?
      file = nil
      if @object_file_cache.key?(obj.file)
        file = @object_file_cache[obj.file]
      else
        tmp = File.join(spec.full_gem_path, obj.file)
        file = tmp if File.exist?(tmp)
        @object_file_cache[obj.file] = file
      end
      return nil if file.nil?
      Solargraph::Location.new(file, Solargraph::Range.from_to(obj.line - 1, 0, obj.line - 1, 0))
    end
  end
end
