# pyterp
import os
import re
import sys


class Interpreter(object):
    operators = []
    operators_regex = None  # Select everything that's not an operator
    program = ""
    source_file = ""

    def __init__(self, filename="main.bf", direct_input=None):
        """
        The heart of the beast. This is where all the logic lives for interpreting a program
        :param filename: The file name to run. It will by default run main.bf
        :param direct_input: Instead of a file to read, you can pass pyterp a string here and it will run it
        :return:
        """
        self.source_file = filename
        if direct_input is not None:
            self.program = self._parse_program(direct_input)
        else:
            self.program = self._load_file(filename)

        self._run()

    def run(self):
        """
        Run it!
        :return:
        """
        self._run()

    def _load_file(self, filename):
        raise NotImplemented("_load_file not implemented for {}".format(self.__class__))

    def _parse_program(self, source_string):
        raise NotImplemented("_pare_program not implemented for {}".format(self.__class__))

    def _run(self):
        raise NotImplemented("_run not implemented for {}".format(self.__class__))


class Brainfuck(Interpreter):
    operators = ['+', '-', ',', '.', '>', '<']
    operators_regex = re.compile('[^\+><\[\],.-]')  # Select everything that's not an operator
    pointer = 0
    tape = [None for x in range(0, 30000)]  # Initialize the tape with null values
    MIN_CELL_SIZE = 0
    MAX_CELL_SIZE = 255

    def _load_file(self, filename):
        try:
            # Try to load the file
            read_file = os.path.realpath(os.path.join(os.curdir, filename))
            with open(read_file, 'rb') as file:
                temp_program = ""
                for line in file:
                    temp_program += line.strip().replace(" ", "").replace("\n", "").replace("\r", "").replace("\t", "")

        except IOError:  # Catch the file load error and exit gracefully
            print "Cannot open file {}".format(filename)
            sys.exit(1)
        return self._parse_program(temp_program)

    def _parse_program(self, source_string):
        """
        This function should strip any whitespace and clean up the input. Our goal is to have a program string of only
        viable operators. By default, we'll ignore any and all non-valid characters instead of throwing a fit
        :return:
        """
        return self.operators_regex.sub('', source_string)

    def _run(self):
        """
        This is the logic to run it all.
        Heavily influenced by this post: http://nickdesaulniers.github.io/blog/2015/05/25/interpreter-compiler-jit/
        :return:
        """
        i = 0
        try:
            while i < len(self.program):
                if self.program[i] == ">":
                    self._increment_pointer()
                elif self.program[i] == "<":
                    self._decrement_pointer()
                elif self.program[i] == "+":
                    self._increment_current_byte()
                elif self.program[i] == "-":
                    self._decrement_current_byte()
                elif self.program[i] == ".":
                    self._output_current_byte()
                elif self.program[i] == ",":
                    self._read_byte()
                elif self.program[i] == "[":
                    """
                    if the byte at the data pointer is zero, then instead of moving the instruction pointer forward to
                    the next command, jump it forward to the command after the matching ] command
                    - Wikipedia
                    """
                    if self.tape[self.pointer] is None or self.tape[self.pointer] == 0:
                        loop = 1
                        while loop > 0:
                            i += 1
                            current_instruction = self.program[i]
                            if current_instruction == "]":
                                loop -= 1
                            elif current_instruction == "[":
                                loop += 1
                elif self.program[i] == "]":
                    """
                    if the byte at the data pointer is nonzero, then instead of moving the instruction pointer
                    forward to the next command, jump it back to the command after the matching [ command.
                    - Wikipedia
                    """
                    if self.tape[self.pointer] is not None and self.tape[self.pointer] > 0:
                        loop = 1
                        while loop > 0:
                            i -= 1
                            current_instruction = self.program[i]
                            if current_instruction == "[":
                                loop -= 1
                            elif current_instruction == "]":
                                loop += 1
                i += 1
        except PointerOutOfProgramRange as e:
            print e.message
            sys.exit(1)
        except IndexError as e:
            print "The program went out of bounds of its instructions"
            sys.exit(1)

    def _increment_pointer(self):
        """
        Increments the internal tape counter by 1
        :raises PointerOutOfProgramRange: Raises an error if the result of incrementing the pointer would bring
        it outside of the tape space on the right
        """
        self.pointer += 1
        if self.pointer >= len(self.tape):
            raise PointerOutOfProgramRange("Pointer exceeded right-hand bound of tape")

    def _decrement_pointer(self):
        """
        Decrements the internal tape counter by 1
        :raises PointerOutOfProgramRange: Raises an error if the result of decrementing the pointer would bring
        it outside of the tape space on the left
        """
        self.pointer -= 1
        if self.pointer < 0:
            raise PointerOutOfProgramRange("Pointer exceeded left-hand bound of tape")

    def _increment_current_byte(self):
        """
        Increments the value of the current byte at the pointer. If the result is over 255,
        then it will overflow to 0
        """
        # If the current byte is uninitialized, then incrementing it will make it 1
        if self.tape[self.pointer] is None:
            self.tape[self.pointer] = 1
        elif self.tape[self.pointer] == self.MAX_CELL_SIZE:  # If the current byte is already at the max, then overflow
            self.tape[self.pointer] = self.MIN_CELL_SIZE
        else:  # increment it
            self.tape[self.pointer] += 1

    def _decrement_current_byte(self):
        """
        Decrements the value of the current byte at the pointer. If the result is below 0,
        then it will overflow to 255
        """
        # If the current byte is uninitialized, then decrementing it will make it the max cell size
        # Otherwise, if it's already at the minimum cell size, then it will also make it the max cell size
        if self.tape[self.pointer] is None or self.tape[self.pointer] == self.MIN_CELL_SIZE:
            self.tape[self.pointer] = self.MAX_CELL_SIZE
        else:  # decrement it
            self.tape[self.pointer] -= 1

    def _output_current_byte(self):
        """
        Prints out the ASCII value of the current byte
        """
        if self.tape[self.pointer] is None:
            print "{}".format(chr(0)),
        else:
            print "{}".format(chr(int(self.tape[self.pointer]))),

    def _read_byte(self):
        """
        Read a single byte from the user without waiting for the \n character
        """
        from .getch import _Getch
        try:
            g = _Getch()
            self.tape[self.pointer] = ord(g())
        except TypeError as e:
            print "Here's what _Getch() is giving me {}".format(g())


class PointerOutOfProgramRange(IndexError):
    """
    Custom exception for when the pointer is out of the range
    """

    def __init__(self, message):
        super(IndexError, self).__init__(message)
        self.message = message
