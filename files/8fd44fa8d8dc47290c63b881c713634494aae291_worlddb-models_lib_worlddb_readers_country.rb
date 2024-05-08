# encoding: UTF-8

module WorldDb

class CountryReader < ReaderBaseWithMoreAttribs

  def read
    reader = ValuesReader.from_string( @text, @more_attribs )

    reader.each_line do |attribs, values|
      opts = { skip_tags: skip_tags? }
      Country.create_or_update_from_attribs( attribs, values, opts )
    end
  end

end # class CountryReader
end # module WorldDb
