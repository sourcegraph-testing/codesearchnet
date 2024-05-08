require_relative 'website'
require_relative 'link'
require_relative 'infrastructure'
require 'yaml'

# Module that creates a link list and generates an html representation
module SycLink

  # Creates a designer that acts as a proxy between the user and the website.
  # The designer will create a website with the provided title.
  class Designer
#    extend Forwardable

#    def_delegators :@website, :report_links_availability

    include Infrastructure

    # The website the designer is working on
    attr_accessor :website

    # Creates a new website where designer can operate on
    def new_website(title = "SYC LINK")
      @website = Website.new(title)
    end

    # Adds a link based on the provided arguments to the website
    def add_link(url, args = {})
      website.add_link(Link.new(url, args))
    end

    # Reads arguments from a CSV file and creates links accordingly. The links
    # are added to the websie
    def add_links_from_file(file)
      File.foreach(file) do |line|
        next if line.chomp.empty?
        url, name, description, tag = line.chomp.split(';')
        website.add_link(Link.new(url, { name: name,
                                         description: description,
                                         tag: tag }))
      end
    end

    # Accepts and SycLink::Importer to import Links and add them to the website
    def import_links(importer)
      importer.links.each do |link|
        website.add_link(link)
      end
    end

    # Export links to specified format
    def export(format)
      message = "to_#{format.downcase}"
      if website.respond_to? message
        website.send(message)
      else
        raise "cannot export to #{format}"
      end
    end

    # List links contained in the website and optionally filter on attributes
    def list_links(args = {})
      website.list_links(args)
    end

    # Finds links based on a search string
    def find_links(search)
      website.find_links(search)
    end

    # Check links availability. Takes a filter which values to return and
    # whether to return available, unavailable or available and unavailable
    # links. In the following example only unavailable links' url and response
    # would be returned
    #   report_links_availability(available: false, 
    #                             unavailable: false,
    #                             columns: "url,response")
    def report_links_availability(opts)
      cols = opts[:columns].gsub(/ /, "").split(',')
      website.report_links_availability.map do |url, response|
        result = if response == "200" and opts[:available]
                   { "url" => url, "response" => response }
                 elsif response != "200" and opts[:unavailable]
                   { "url" => url, "response" => response }
                 end
        next if result.nil?
        cols.inject([]) { |res, c| res << result[c.downcase] }
      end.compact
    end

    # Updates a link. The link is identified by the URL. If there is more than
    # one link with the same URL, only the first link is updated
    def update_link(url, args)
      website.find_links(url).first.update(args)
    end

    # Updates links read from a file
    def update_links_from_file(file)
      File.foreach(file) do |line|
        next if line.chomp.empty?
        url, name, description, tag = line.chomp.split(';')
        website.find_links(url).first.update({ name:        name,
                                               description: description,
                                               tag:         tag })
      end
    end

    # Merge links with same URL
    def merge_links
      website.merge_links_on(:url)
    end

    # Deletes one or more links from the website. Returns the deleted links.
    # Expects the links provided in an array
    def remove_links(urls)
      urls.map { |url| list_links({ url: url }) }.flatten.compact.each do |link|
        website.remove_link(link)
      end
    end

    # Deletes links based on URLs that are provided in a file. Each URL has to
    # be in one line
    def remove_links_from_file(file)
      urls = File.foreach(file).map { |url| url.chomp unless url.empty? }
      remove_links(urls)
    end

    # Saves the website to the specified directory with the downcased name of
    # the website and the extension 'website'. The website is save as YAML
    def save_website(directory)
      File.open(yaml_file(directory, website.title), 'w') do |f|
        YAML.dump(website, f)
      end
    end

    # Loads a website based on the provided YAML-file and asigns it to the
    # designer to operate on
    def load_website(website)
      @website = YAML.load_file(website)
    end

    # Deletes the website if it already exists
    def delete_website(directory)
      if File.exists? yaml_file(directory, website.title)
        FileUtils.rm(yaml_file(directory, website.title)) 
      end
    end

    # Creates the html representation of the website. The website is saved to
    # the directory with websites title and needs an erb-template where the
    # links are integrated to. An example template can be found at
    # templates/syclink.html.erb
    def create_website(directory, template_filename)
      template = File.read(template_filename)
      File.open(html_file(directory, website.title), 'w') do |f|
        f.puts website.to_html(template)
      end 
    end
 
  end

end
