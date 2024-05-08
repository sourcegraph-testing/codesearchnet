# frozen_string_literal: true

require "net/http"

require "server_sent_events/event"

module ServerSentEvents
  # Client is a class whose responsibility is to stream the response data from
  # the server, feed that data into parser and call user supplied block when
  # events are detected.
  class Client
    # Create new SSE client.
    #
    # Note that creating new client does not establish connection to the
    # server. Connection is established by the {#listen} call and torn down
    # automatically when {#listen returns}.
    #
    # @param address [URI] endpoint to connect to
    # @param parser [Parser] object that should be used to parse incoming data
    # @param headers [Hash] additional headers that should be added to request
    def initialize(address, parser, headers = {})
      @address = address
      @headers = headers
      @parser = parser
    end

    # Listen for events from the server.
    #
    # To perform some action when event arrives, specify a block that gets
    # executed eact time new event is availble like this:
    #
    #     client.listen { |e| puts e }
    def listen
      Net::HTTP.start(@address.host, @address.port) do |http|
        # TODO(@tadeboro): Add support for adding custom headers (auth)
        http.request(Net::HTTP::Get.new(@address)) do |response|
          # TODO(@tadeboro): Handle non-200 here
          response.read_body do |chunk|
            @parser.push(chunk).each { |e| yield(e) }
          end
        end
      end
    end
  end
end
