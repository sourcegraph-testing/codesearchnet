=begin
  * Name: ActionEvent
  * Description: Event used to notify interested parties that an action has happened on component
                 Usually a button press. Nothing more.
  * Author: jkepler (ABCD)
  
  --------
  * Date: 2010-09-12 18:53 
  * License:
    Same as Ruby's License (http://www.ruby-lang.org/LICENSE.txt)

=end

# Event created when state changed (as in ViewPort)
module Canis
  # source - as always is the object whose event has been fired
  # id     - event identifier (seems redundant since we bind events often separately.
  # event  - is :PRESS
  # action_command - command string associated with event (such as title of button that changed
  ActionEvent = Struct.new(:source, :event, :action_command) do
    # This should always return the most relevant text associated with this object
    # so the user does not have to go through the source object's documentation.
    # It should be a user-friendly string 
    # @return text associated with source (label of button)
    def text
      source.text
    end

    # This is similar to text and can often be just an alias.
    # However, i am putting this for backward compatibility with programs
    # that received the object and called it's getvalue. It is better to use text.
    # @return text associated with source (label of button)
    def getvalue
      source.getvalue
    end
  end
  # a derivative of Action Event for textviews
  # We allow a user to press ENTER on a row and use that for processing.
  # We are basically using TextView as a list in which user can scroll around
  # and move cursor at will.
  class TextActionEvent < ActionEvent
    # current_index or line number starting 0
    attr_accessor :current_index
    # cursor position on the line
    attr_accessor :curpos
    def initialize source, event, action_command, current_index, curpos
      super source, event, action_command
      @current_index = current_index
      @curpos = curpos
    end
    # the text of the line on which the user is
    def text
      source.current_value.to_s
    end
    # the word under the cursor TODO
    # if its a text with pipe delim, then ??
    def word_under_cursor line=text(), pos=@curpos, delim=" "
      line ||= text()
      pos ||= @curpos
      # if pressed on a space, try to go to next word to make easier 2013-03-24
      if line[pos,1] == delim
        while line[pos,1] == delim
          pos += 1
        end
      end
      finish = line.index(delim, pos)
      start = line.rindex(delim,pos)
      finish = -1 if finish.nil?
      start = 0 if start.nil?
      return line[start..finish]
    end
  end
end
