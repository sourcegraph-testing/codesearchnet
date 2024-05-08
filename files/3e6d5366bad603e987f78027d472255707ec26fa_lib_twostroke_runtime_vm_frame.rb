require "#{__FILE__}/../vm"

module Twostroke::Runtime
  class VM::Frame
    attr_reader :vm, :insns, :stack, :sp_stack, :ex_stack, :enum_stack, :exception, :ip, :scope
    
    def initialize(vm, section, callee = nil)
      @vm = vm
      @section = section
      @insns = vm.bytecode[section]
      @callee = callee
    end
    
    def arguments_object
      arguments = Types::Object.new
      @args.each_with_index { |arg,i| arguments.put i.to_s, arg }
      arguments.put "length", Types::Number.new(@args.size)
      arguments.put "callee", @callee
      arguments
    end
    
    def execute(scope, this = nil, args = [])
      @scope = scope || vm.global_scope
      @stack = []
      @sp_stack = []
      @ex_stack = []
      @exception = nil
      @enum_stack = []
      @temp_slot = nil
      @ip = 0
      @return = false
      @return_after_finally = false
      @this = this || @scope.global_scope.root_object
      @args = args
      if @callee
        # the arguments object is only available within functions
        scope.declare :arguments
        scope.set_var :arguments, arguments_object
      end
      
      until @return or @ip >= insns.size
        ins, arg = insns[@ip]
        if vm.instruction_trace
          # if an instruction trace callback is set, call it with information about this ins
          vm.instruction_trace.call @section, @ip, ins, arg
        end
        @ip += 1
        if ex = catch(:exception) { send ins, arg; nil }
          @exception = ex
          if ex.respond_to? :data and ex.data[:exception_stack]
            ex.data[:exception_stack] << "at #{@this._class && @this._class.name}.#{@name || "(anonymous function)"}:#{@line}  <#{@section}+#{@ip}>"
          end
          throw :exception, @exception if ex_stack.empty?
          @ip = ex_stack.last[:catch] || ex_stack.last[:finally]
        end
      end
      
      @return
    end
    
    define_method ".line" do |arg|
      @line = arg
      if vm.line_trace
        vm.line_trace.call @section, @line
      end
    end
    
    define_method ".name" do |arg|
      @name = arg
      scope.declare arg.intern
      scope.set_var arg.intern, @callee
    end
    
    define_method ".local" do |arg|
      scope.declare arg.intern
    end
    
    define_method ".arg" do |arg|
      scope.declare arg.intern
      scope.set_var arg.intern, @args.shift || Types::Undefined.new
    end
    
    define_method ".catch" do |arg|
      ex_stack.last[:catch] = nil
      scope.declare arg.intern
      scope.set_var arg.intern, @exception
      @exception = nil
    end
    
    define_method ".finally" do |arg|
      ex_stack.pop
    end
    
    ## instructions
    
    def push(arg)
      if arg.is_a? Symbol
        stack.push scope.get_var(arg)
      elsif arg.is_a?(Fixnum) || arg.is_a?(Float)
        stack.push Types::Number.new(arg)
      elsif arg.is_a?(String)
        stack.push Types::String.new(arg)
      end
    end
    
    def call(arg)
      args = []
      arg.times { args.unshift @stack.pop }
      fun = stack.pop
      Lib.throw_type_error "called non callable" unless fun.respond_to?(:call)
      stack.push fun.call(scope, fun.inherits_caller_this ? @this : scope.global_scope.root_object, args)
    end
    
    def thiscall(arg)
      args = []
      arg.times { args.unshift stack.pop }
      fun = stack.pop
      Lib.throw_type_error "called non callable" unless fun.respond_to?(:call)
      this_arg = Types.to_object stack.pop
      stack.push fun.call(scope, fun.inherits_caller_this ? @this : this_arg, args)
    end
    
    def methcall(arg)
      args = []
      arg.times { args.unshift stack.pop }
      prop = Types.to_string(stack.pop).string
      obj = Types.to_object stack.pop
      fun = obj.get prop
      unless fun.respond_to? :call
        fun = obj.get "__noSuchMethod__"
        Lib.throw_type_error "called non callable" unless fun.respond_to? :call
        args = [Types::String.new(prop), Types::Array.new(args)]
      end
      stack.push fun.call(scope, fun.inherits_caller_this ? @this : obj, args)
    end
    
    def newcall(arg)
      args = []
      arg.times { args.unshift @stack.pop }
      fun = stack.pop
      Lib.throw_type_error "called non callable" unless fun.respond_to?(:call)
      obj = Types::Object.new
      obj.construct prototype: fun.get("prototype"), _class: fun do
        retn = fun.call(scope, obj, args)
        if retn.is_a?(Types::Undefined)
          stack.push obj
        else
          stack.push retn
        end
      end
    end
    
    def dup(arg)
      n = arg || 1
      stack.push *stack[-n..-1]
    end
    
    def tst(arg)
      @temp_slot = stack.pop
    end
    
    def tld(arg)
      stack.push @temp_slot
    end
    
    def member(arg)
      stack.push Types.to_object(stack.pop).get(arg.to_s)
    end
    
    def deleteg(arg)
      scope.delete arg
      stack.push Types::Boolean.true
    end
    
    def delete(arg)
      Types.to_object(stack.pop).delete arg.to_s
      stack.push Types::Boolean.true
    end
    
    def deleteindex(arg)
      obj = Types.to_object stack.pop
      idx = Types.to_string stack.pop
      obj.delete idx.string
      stack.push Types::Boolean.true
    end
    
    def in(arg)
      obj = Types.to_object stack.pop
      idx = Types.to_string stack.pop
      stack.push Types::Boolean.new(obj.has_property idx.string)
    end
    
    def enum(arg)
      props = []
      obj = stack.pop
      Types.to_object(obj).each_enumerable_property { |p| props.push p } unless obj.is_a?(Types::Null) || obj.is_a?(Types::Undefined)
      @enum_stack.push [props, 0]
    end

    def enumnext(arg)
      enum = @enum_stack.last
      stack.push Types::String.new(enum[0][enum[1]])
      enum[1] += 1
    end
    
    def jiee(arg)
      enum = @enum_stack.last
      @ip = arg if enum[1] >= enum[0].size
    end
    
    def popenum(arg)
      @enum_stack.pop
    end
    
    def set(arg)
      scope.set_var arg, stack.last
    end
    
    def setprop(arg)
      val = stack.pop
      obj = Types.to_object(stack.pop)
      obj.put arg.to_s, val
      stack.push val
    end
    
    def ret(arg)
      if ex_stack.empty?
        @return = stack.pop
      else
        @return_after_finally = stack.pop
        @ip = ex_stack.last[:finally]
      end
    end
    
    def _throw(arg)
      throw :exception, stack.pop
    end
    
    def eq(arg)
      b = stack.pop
      a = stack.pop
      stack.push Types::Boolean.new(Types.eq(a, b))
    end
    
    def seq(arg)
      b = stack.pop
      a = stack.pop
      stack.push Types::Boolean.new(Types.seq(a, b))
    end
    
    def null(arg)
      stack.push Types::Null.new
    end
    
    def true(arg)
      stack.push Types::Boolean.true
    end
    
    def false(arg)
      stack.push Types::Boolean.false
    end
    
    def jmp(arg)
      @ip = arg.to_i
    end
    
    def jif(arg)
      if Types.is_falsy stack.pop
        @ip = arg.to_i
      end
    end
    
    def jit(arg)
      if Types.is_truthy stack.pop
        @ip = arg.to_i
      end
    end
    
    def not(arg)
      stack.push Types::Boolean.new(Types.is_falsy(stack.pop))
    end
    
    def inc(arg)
      stack.push Types::Number.new(Types.to_number(stack.pop).number + 1)
    end
    
    def dec(arg)
      stack.push Types::Number.new(Types.to_number(stack.pop).number - 1)
    end
    
    def pop(arg)
      stack.pop
    end
    
    def index(arg)
      index = Types.to_string(stack.pop).string
      stack.push(Types.to_object(stack.pop).get(index) || Types::Undefined.new)
    end
    
    def array(arg)
      args = []
      arg.times { args.unshift stack.pop }
      stack.push Types::Array.new(args)
    end
    
    def undefined(arg)
      stack.push Types::Undefined.new
    end
    
    def number(arg)
      stack.push Types.to_number(stack.pop)
    end
    
    def regexp(arg)
      stack.push Types::RegExp.new(*arg)
    end
    
    def sal(arg)
      r = Types.to_uint32(stack.pop) & 31
      l = Types.to_int32 stack.pop
      stack.push Types::Number.new(l << r)
    end
    
    def slr(arg)
      r = Types.to_uint32(stack.pop) & 31
      l = Types.to_int32 stack.pop
      stack.push Types::Number.new(l >> r)
    end
    
    def sar(arg)
      r = Types.to_uint32(stack.pop) & 31
      l = Types.to_uint32 stack.pop
      stack.push Types::Number.new(l >> r)
    end
    
    def add(arg)
      r = stack.pop
      l = stack.pop
      right = Types.to_primitive r
      left = Types.to_primitive l
      
      if left.is_a?(Types::String) || right.is_a?(Types::String)
        stack.push Types::String.new(Types.to_string(left).string + Types.to_string(right).string)
      else
        stack.push Types::Number.new(Types.to_number(left).number + Types.to_number(right).number)
      end
    end
    
    def sub(arg)
      right = Types.to_number(stack.pop).number
      left = Types.to_number(stack.pop).number
      stack.push Types::Number.new(left - right)
    end
    
    def mul(arg)
      right = Types.to_number(stack.pop).number
      left = Types.to_number(stack.pop).number
      stack.push Types::Number.new(left * right)
    end
    
    def div(arg)
      right = Types.to_number(stack.pop).number
      left = Types.to_number(stack.pop).number
      stack.push Types::Number.new(left / right.to_f)
    end
    
    def mod(arg)
      right = Types.to_number(stack.pop).number
      left = Types.to_number(stack.pop).number
      stack.push Types::Number.new(left % right)
    end
    
    def and(arg)
      right = Types.to_int32 stack.pop
      left = Types.to_int32 stack.pop
      stack.push Types::Number.new(left & right)
    end
    
    def or(arg)
      right = Types.to_int32 stack.pop
      left = Types.to_int32 stack.pop
      stack.push Types::Number.new(left | right)
    end
    
    def xor(arg)
      right = Types.to_int32 stack.pop
      left = Types.to_int32 stack.pop
      stack.push Types::Number.new(left ^ right)
    end
    
    def bnot(arg)
      val = Types.to_int32 stack.pop
      stack.push Types::Number.new ~val
    end
    
    def setindex(arg)
      val = stack.pop
      index = Types.to_string(stack.pop).string
      Types.to_object(stack.pop).put index, val
      stack.push val
    end
    
    def lt(arg)
      comparison_oper :<
    end
    
    def lte(arg)
      comparison_oper :<=
    end
    
    def gt(arg)
      comparison_oper :>
    end
    
    def gte(arg)
      comparison_oper :>=
    end
    
    def typeof(arg)
      if arg
        stack.push Types::String.new(scope.has_var(arg) ? scope.get_var(arg).typeof : "undefined")
      else
        stack.push Types::String.new(stack.pop.typeof)
      end
    end
    
    def instanceof(arg)
      r = stack.pop
      l = stack.pop
      stack.push Types::Boolean.new(r.has_instance l)
    end
    
    def close(arg)
      name, arguments = vm.section_name_args arg
      scope = @scope
      fun = Types::Function.new(->(outer_scope, this, args) { VM::Frame.new(vm, arg, fun).execute(scope.close, this, args) }, "...", name || "", arguments)
      stack.push fun
    end
    
    def callee(arg)
      stack.push @callee
    end
    
    def object(arg)
      obj = Types::Object.new
      kvs = []
      arg.reverse_each { |a| kvs << [a, stack.pop] }
      kvs.reverse_each { |kv| obj.put kv[0].to_s, kv[1] }
      stack.push obj
    end
    
    def negate(arg)
      n = Types.to_number(stack.pop).number
      if n.zero?
        stack.push Types::Number.new(-n.to_f) # to preserve javascript's 0/-0 semantics
      else
        stack.push Types::Number.new(-n)
      end
    end
    
    def with(arg)
      @scope = ObjectScope.new stack.pop, @scope
    end
    
    def popscope(arg)
      @scope = @scope.parent
    end
    
    def pushsp(arg)
      sp_stack.push stack.size
    end
    
    def popsp(arg)
      @stack = stack[0...sp_stack.pop]
    end
    
    def pushexh(arg)
      ex_stack.push catch: arg[0], finally: arg[1]
    end
    
    def popexh(arg)
      exh = ex_stack.pop
      @ip = exh[:finally]
    end
    
    def popcat(arg)
      ex_stack.last[:catch] = nil
      @ip = ex_stack.last[:finally]
    end
    
    def popfin(arg)
      throw :exception, @exception if @exception
      if @return_after_finally
        @return = @return_after_finally
        @return_after_finally = nil
      end
    end
    
    def this(arg)
      stack.push @this
    end
    
  private
    def comparison_oper(op)
      right = Types.to_primitive stack.pop
      left = Types.to_primitive stack.pop
      
      if left.is_a?(Types::String) && right.is_a?(Types::String)
        stack.push Types::Boolean.new(left.string.send op, right.string)
      else
        stack.push Types::Boolean.new(Types.to_number(left).number.send op, Types.to_number(right).number)
      end
    end
  
    def error!(msg)
      vm.send :error!, "#{msg} (at #{@section}+#{@ip - 1})"
    end
  end
end