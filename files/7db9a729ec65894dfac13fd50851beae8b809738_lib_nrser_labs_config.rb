# encoding: UTF-8
# frozen_string_literal: true

# Requirements
# =======================================================================

# Stdlib
# -----------------------------------------------------------------------

# Deps
# -----------------------------------------------------------------------

# Project / Package
# -----------------------------------------------------------------------


# Refinements
# =======================================================================


# Namespace
# =======================================================================

module  NRSER


# Definitions
# =======================================================================

# @todo document Config class.
class Config
  
  # Constants
  # ========================================================================

  
  # Mixins
  # ========================================================================

  include Hamster::Immutable
  include Hamster::Enumerable
  include Hamster::Associable
  
  
  # Class Methods
  # ========================================================================
  
  def self.deep_trie_merge trie, source, &block
    to_put = {}

    source.each_pair do |key, source_value|
      _, trie_value = trie.get key

      to_put[ key ] = if [ trie_value, source_value ].all? { |value|
        NRSER.hash_like?( value ) && value.respond_to?( :deep_merge )
      }
        trie_value.deep_merge source_value, &block
      elsif block
        block.call key, trie_value, source_value
      else
        source_value
      end
    end

    trie.bulk_put to_put
  end # .deep_trie_merge

  
  # Attributes
  # ========================================================================
  
  
  # Construction
  # ========================================================================
  
  # Instantiate a new `Config`.
  def initialize *sources
    @sources = sources
    @trie = sources.reduce( Hamster::EmptyTrie ) { |trie, source|
      self.class.deep_trie_merge trie, source
    }

  end # #initialize
  
  
  # Instance Methods
  # ========================================================================

  # Retrieve the value corresponding to the provided key object. If not found,
  # and this `Hash` has a default block, the default block is called to provide
  # the value. Otherwise, return `nil`.
  #
  # @example
  #   h = Hamster::Hash["A" => 1, "B" => 2, "C" => 3]
  #   h["B"]             # => 2
  #   h.get("B")         # => 2
  #   h.get("Elephant")  # => nil
  #
  #   # Hamster Hash with a default proc:
  #   h = Hamster::Hash.new("A" => 1, "B" => 2, "C" => 3) { |key| key.size }
  #   h.get("B")         # => 2
  #   h.get("Elephant")  # => 8
  #
  # @param [Array<Object>] key_path
  #   The key to look up
  # @return [Object]
  # 
  def get *key_path, type: nil, default: nil
    first, *rest = key_path
    entry = @trie.get first

    value = if entry
      current = entry[ 1 ]

      while !current.nil? && !rest.empty?
        first, *rest = rest

        current = if current.respond_to? :dig
          rest = [] # Short-circuit
          current.dig *rest

        elsif current.respond_to? :[]
          current[ first ]
        
        elsif current.respond_to? :get
          current.get first
        
        else
          rest = []
          default

        end
      end

      current

    else
      default
    end

    parse_and_check key_path, value, type: type

  end
  alias :[] :get
  alias :dig :get


  def parse_and_check key_path, value, type: nil
    return value if type.nil?
  end


  def each *args, &block
    @trie.each *args, &block
  end
  
  
end # class Config

# /Namespace
# =======================================================================

end # module NRSER
