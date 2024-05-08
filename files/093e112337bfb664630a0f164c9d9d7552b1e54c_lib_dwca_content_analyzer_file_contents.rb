# frozen_string_literal: true

require 'csv'

require_relative 'column'
require_relative 'csv_converters'

#
module DwCAContentAnalyzer
  #
  class FileContents
    attr_reader :columns

    def initialize(file, detectors = %i[type length])
      @file = file
      @detectors = detectors
      @columns = analyze
    end

    private

    def analyze
      table = load_table @file
      table.by_col!.map do |col|
        header = col[0]
        contents = col[1]
        Column.new(header.to_i, contents, *@detectors)
      end
    end

    # reads the first line of the CSV file
    # returns the columns indices as an array
    def headers(file)
      Array.new(CSV.open(file, &:readline).size) { |i| i.to_s }
    end

    def load_table(file)
      CSV.read(file,
               headers: headers(file),
               converters: %i[blank_to_nil safe_numeric date])
    end
  end
end
