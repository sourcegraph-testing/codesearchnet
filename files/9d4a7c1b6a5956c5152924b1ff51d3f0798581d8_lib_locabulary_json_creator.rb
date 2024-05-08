require "google_drive"
require 'highline/import'
require 'locabulary/utility'
require 'locabulary/item'
require 'json'

module Locabulary
  # Responsible for capturing predicate_name from a given source and writing it to a file
  class JsonCreator
    def initialize(document_key, predicate_name, data_fetcher = default_data_fetcher)
      @document_key = document_key
      @predicate_name = predicate_name
      @output_filepath = Utility.filename_for_predicate_name(predicate_name)
      @data_fetcher = data_fetcher
    end

    attr_reader :document_key, :predicate_name, :data_fetcher, :spreadsheet_data, :json_data
    attr_accessor :output_filepath

    def create_or_update
      rows = data_fetcher.call(document_key)
      data = extract_data_from(rows)
      convert_to_json(data)
    end

    # :nocov:
    def write_to_file
      File.open(output_filepath, "w") do |f|
        f.puts json_data
      end
    end
    # :nocov:

    private

    def extract_data_from(rows)
      spreadsheet_data = []
      header = rows[0]
      rows[1..-1].each do |row|
        # The activated_on is a present hack reflecting a previous value
        row_data = { "predicate_name" => predicate_name, "activated_on" => "2015-07-22", "default_presentation_sequence" => nil }
        row.each_with_index do |cell, index|
          row_data[header[index]] = cell unless cell.to_s.strip == ''
        end
        spreadsheet_data << row_data
      end
      spreadsheet_data
    end

    def default_data_fetcher
      ->(document_key) { GoogleSpreadsheet.new(document_key).all_rows }
    end

    def convert_to_json(data)
      json_array = data.map do |row|
        Locabulary::Item.build(row).to_h
      end
      @json_data = JSON.pretty_generate("predicate_name" => predicate_name, "values" => json_array)
    end

    # :nocov:
    # Responsible for building credentials from code
    module GoogleAccessTokenFetcher
      OOB_URI = "urn:ietf:wg:oauth:2.0:oob".freeze
      READ_ONLY_SCOPE = "https://www.googleapis.com/auth/drive.readonly".freeze
      def self.call
        # This looks a bit funny in that we can cache the tokens that are returned. However I don't want to do that.
        # So instead, I'm adding a symbol that should barf if the underlying interface changes.
        token_store = :token_store

        client_id = Google::Auth::ClientId.new(client_secrets.fetch('client_id'), client_secrets.fetch('client_secret'))
        authorizer = Google::Auth::UserAuthorizer.new(client_id, READ_ONLY_SCOPE, token_store)
        authorization_url = authorizer.get_authorization_url(base_url: OOB_URI)
        puts "\n Open the following URL, login with your credentials and get the authorization code \n\n #{authorization_url}\n\n"
        authorization_code = ask('Authorization Code: ')
        authorizer.get_credentials_from_code(base_url: OOB_URI, code: authorization_code)
      end

      def self.client_secrets
        @secrets ||= YAML.safe_load(File.open(File.join(secrets_path)))
      end

      def self.secrets_path
        if File.exist? File.join(File.dirname(__FILE__), '../../config/client_secrets.yml')
          File.join(File.dirname(__FILE__), '../../config/client_secrets.yml')
        else
          File.join(File.dirname(__FILE__), '../../config/client_secrets.example.yml')
        end
      end
    end

    # Responsible for interacting with Google Sheets and retrieiving relevant information
    class GoogleSpreadsheet
      attr_reader :access_token, :document_key, :session

      private :session

      def initialize(document_key, access_token_fetcher = GoogleAccessTokenFetcher)
        @document_key = document_key
        @access_token = access_token_fetcher.call
        @session = GoogleDrive.login_with_oauth(access_token)
      end

      def all_rows
        session.spreadsheet_by_key(document_key).worksheets[0].rows
      end
    end
  end
end
