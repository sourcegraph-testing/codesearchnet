module TweetToSounds
  class KeywordExtractor
    STOPWORDS = %w(
      a an and are as at be but by for if in into is it no not of on or such
      that the their then there these they this to was will with
    )

    TWO_LETTERS = %w(
      ab ad ah am aw ax ay bi by do ed eh el er es et ex fa go ha he hi hm ho
      id jo la lo ma me mi mm my no oh om ow ox oy pa pe pi re sh si so ta ti
      uh um un up us we wo ya ye yo
    )

    attr_reader :keywords

    def initialize(tweet)
      @tweet    = tweet
      @keywords = extract_keywords
    end

    private

    def extract_keywords
      words = @tweet.downcase.gsub(/[^\s\w\-']/, '').split
      words = words.uniq - STOPWORDS
      words.select { |w| w.length > 2 || TWO_LETTERS.include?(w) }
    end
  end
end
