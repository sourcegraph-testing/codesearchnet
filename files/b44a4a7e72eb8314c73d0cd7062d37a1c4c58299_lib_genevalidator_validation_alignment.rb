require 'bio'
require 'forwardable'

require 'genevalidator/exceptions'
require 'genevalidator/get_raw_sequences'
require 'genevalidator/validation_report'
require 'genevalidator/validation_test'

module GeneValidator
  ##
  # Class that stores the validation output information
  class AlignmentValidationOutput < ValidationReport
    attr_reader :gaps
    attr_reader :extra_seq
    attr_reader :consensus
    attr_reader :threshold
    attr_reader :result

    def initialize(short_header, header, description, gaps = 0, extra_seq = 0,
                   consensus = 1, threshold = 20, expected = :yes)

      @short_header = short_header
      @header = header
      @description = description
      @gaps         = (gaps * 100).round.to_s + '%'
      @extra_seq    = (extra_seq * 100).round.to_s + '%'
      @consensus    = (consensus * 100).round.to_s + '%'
      @threshold    = threshold
      @result       = validation
      @expected     = expected
      @plot_files   = []
      @approach     = 'We expect the query sequence to be similar to the top' \
                      ' ten BLAST hits. Here, we create a statistical' \
                      ' consensus model of those top hits and compare the' \
                      ' query to this model.'
      @explanation  = "The query sequence includes #{@consensus} amino-acid" \
                      ' residues present in the consensus model.' \
                      " #{@extra_seq} of residues in the query sequence are" \
                      ' absent from the consensus profile. ' \
                      " #{@gaps} of residues in the consensus profile are" \
                      ' absent from the query sequence.'
      @conclusion   = conclude
    end

    def conclude
      if @result == :yes
        'There is no evidence based on the top 10 BLAST hits to suggest any' \
        ' problems with the query sequence.'
      else
        t = 'These results suggest that there may be some problems with' \
            ' the query sequence.'
        t1 = ''
        t2 = ''
        t3 = '' # Create empty string variables
        if (1 - consensus.to_i) > @threshold
          t1 = ' There is low conservation of residues between the' \
               ' statistical profile and the query sequence (the cut-off' \
               ' is 80%).'
        end
        if extra_seq.to_i > @threshold
          t2 = " The query sequence has a high percentage (#{@extra_seq})" \
               ' of extra residues absent from the statistical profile' \
               ' (the cut-off is 20%).'
        end
        if gaps.to_i > @threshold
          t3 = " The query sequence has a high percentage (#{@gaps}) of" \
               ' missing residues when compared to the statistical profile' \
               ' (the cut-off is 20%).'
        end
        t + t1 + t2 + t3
      end
    end

    def print
      "#{@consensus}&nbsp;conserved; #{@extra_seq}&nbsp;extra;" \
      " #{@gaps}&nbsp;missing."
    end

    def validation
      if gaps.to_i < @threshold && extra_seq.to_i < @threshold &&
         (1 - consensus.to_i) < @threshold
        :yes
      else
        :no
      end
    end
  end

  ##
  # This class contains the methods necessary for
  # validations based on multiple alignment
  class AlignmentValidation < ValidationTest
    extend Forwardable
    def_delegators GeneValidator, :opt, :config
    attr_reader :multiple_alignment
    attr_reader :raw_seq_file
    attr_reader :index_file_name
    attr_reader :raw_seq_file_load

    ##
    # Initilizes the object
    # Params:
    # +prediction+: a +Sequence+ object representing the blast query
    # +hits+: a vector of +Sequence+ objects (representing blast hits)
    # +plot_path+: name of the fasta file
    def initialize(prediction, hits)
      super
      @short_header       = 'MissingExtraSequences'
      @cli_name           = 'align'
      @header             = 'Missing/Extra Sequences'
      @description        = 'Finds missing and extra sequences in the' \
                            ' prediction, based on the multiple alignment of' \
                            ' the best hits. Also counts the percentage of' \
                            ' the conserved regions that appear in the' \
                            ' prediction.'
      @raw_seq_file       = opt[:raw_sequences]
      @index_file_name    = config[:raw_seq_file_index]
      @raw_seq_file_load  = config[:raw_seq_file_load]
      @db                 = opt[:db]
      @multiple_alignment = []
      @num_threads        = opt[:mafft_threads]
      @type               = config[:type]
    end

    ##
    # Find gaps/extra regions based on the multiple alignment
    # of the first n hits
    # Output:
    # +AlignmentValidationOutput+ object
    def run
      n = opt[:min_blast_hits] < 10 ? 10 : opt[:min_blast_hits]
      n = 50 if n > 50

      raise NotEnoughHitsError if hits.length < n
      raise unless prediction.is_a?(Query) && hits[0].is_a?(Query)

      start = Time.new
      # get the first n hits
      n_hits = [n - 1, @hits.length].min
      less_hits = @hits[0..n_hits]
      # get raw sequences for less_hits
      less_hits.delete_if do |hit|
        if hit.raw_sequence.nil?
          hit.raw_sequence = FetchRawSequences.run(hit.identifier,
                                                   hit.accession_no)
        end
        hit.raw_sequence.nil? ? true : false
      end

      raise NoInternetError if less_hits.length.zero?
      # in case of nucleotide prediction sequence translate into protein
      # translate with the reading frame of all hits considered for alignment
      reading_frames = less_hits.map(&:reading_frame).uniq
      raise ReadingFrameError if reading_frames.length != 1

      if @type == :nucleotide
        s = Bio::Sequence::NA.new(prediction.raw_sequence)
        prediction.protein_translation = s.translate(reading_frames[0])
      end

      # multiple align sequences from less_hits with the prediction
      # the prediction is the last sequence in the vector
      @multiple_alignment = multiple_align_mafft(prediction, less_hits)

      out = get_sm_pssm(@multiple_alignment[0..@multiple_alignment.length - 2])
      sm = out[0]
      freq = out[1]

      # remove isolated residues from the predicted sequence
      index          = @multiple_alignment.length - 1
      prediction_raw = remove_isolated_residues(@multiple_alignment[index])
      # remove isolated residues from the statistical model
      sm = remove_isolated_residues(sm)

      a1 = get_consensus(@multiple_alignment[0..@multiple_alignment.length - 2])

      plot1     = plot_alignment(freq)
      gaps      = gap_validation(prediction_raw, sm)
      extra_seq = extra_sequence_validation(prediction_raw, sm)
      consensus = consensus_validation(prediction_raw, a1)

      @validation_report = AlignmentValidationOutput.new(@short_header, @header,
                                                         @description, gaps,
                                                         extra_seq, consensus)
      @validation_report.plot_files.push(plot1)
      @validation_report.run_time = Time.now - start
      @validation_report
    rescue NotEnoughHitsError
      @validation_report = ValidationReport.new('Not enough evidence',
                                                :warning, @short_header,
                                                @header, @description)
    rescue NoMafftInstallationError
      @validation_report = ValidationReport.new('Mafft error', :error,
                                                @short_header, @header,
                                                @description)
      @validation_report.errors.push NoMafftInstallationError
    rescue NoInternetError
      @validation_report = ValidationReport.new('Internet error', :error,
                                                @short_header, @header,
                                                @description)
      @validation_report.errors.push NoInternetError
    rescue ReadingFrameError
      @validation_report = ValidationReport.new('Multiple reading frames',
                                                :error, @short_header,
                                                @header, @description)
      @validation_report.errors.push 'Multiple reading frames Error'
    rescue StandardError
      @validation_report = ValidationReport.new('Unexpected error', :error,
                                                @short_header, @header,
                                                @description)
      @validation_report.errors.push 'Unexpected Error'
    end

    ##
    # Builds the multiple alignment between
    # all the hits and the prediction
    # using MAFFT tool
    # Also creates a fasta file with the alignment
    # Params:
    # +prediction+: a +Sequence+ object representing the blast query
    # +hits+: a vector of +Sequience+ objects (usually representing blast hits)
    # +path+: path of mafft installation
    # Output:
    # Array of +String+s, corresponding to the multiple aligned sequences
    # the prediction is the last sequence in the vector
    def multiple_align_mafft(prediction, hits)
      raise unless prediction.is_a?(Query) && hits[0].is_a?(Query)

      opt = ['--maxiterate', '1000', '--localpair', '--anysymbol', '--quiet',
             '--thread', @num_threads.to_s]
      mafft = Bio::MAFFT.new('mafft', opt)
      sequences = hits.map do |h|
        # remove the seq id - as MAFFT sometimes has an issue with this
        f = Bio::FastaFormat.new(h.raw_sequence)
        # check if fasta sequence otherwise returne original entry
        f.seq.empty? ? h.raw_sequence : f.seq
      end
      sequences.push(prediction.protein_translation)

      report = mafft.query_align(sequences)
      alignment = report.alignment.map(&:to_s)
      raise NoMafftInstallationError if alignment.empty?
      alignment
    rescue StandardError
      raise NoMafftInstallationError
    end

    ##
    # Returns the consensus regions among
    # a set of multiple aligned sequences
    # i.e positions where there is the same
    # element in all sequences
    # Params:
    # +ma+: array of +String+s, corresponding to the multiple aligned sequences
    # Output:
    # +String+ with the consensus regions
    def get_consensus(ma)
      align = Bio::Alignment.new(ma)
      align.consensus
    end

    ##
    # Returns the percentage of gaps in the prediction
    # with respect to the statistical model
    # Params:
    # +prediction+: +String+ corresponding to the prediction sequence
    # +sm+: +String+ corresponding to the statistical model
    # Output:
    # +Fixnum+ with the score
    def gap_validation(prediction_raw, sm)
      return 1 if prediction_raw.length != sm.length
      # find gaps in the prediction and
      # not in the statistical model
      no_gaps = 0
      (0..sm.length - 1).each do |i|
        no_gaps += 1 if prediction_raw[i] == '-' && sm[i] != '-'
      end
      no_gaps / (sm.length + 0.0)
    end

    ##
    # Returns the percentage of extra sequences in the prediction
    # with respect to the statistical model
    # Params:
    # +prediction+: +String+ corresponding to the prediction sequence
    # +sm+: +String+ corresponding to the statistical model
    # Output:
    # +Fixnum+ with the score
    def extra_sequence_validation(prediction_raw, sm)
      return 1 if prediction_raw.length != sm.length
      # find residues that are in the prediction
      # but not in the statistical model
      no_insertions = 0
      (0..sm.length - 1).each do |i|
        no_insertions += 1 if prediction_raw[i] != '-' && sm[i] == '-'
      end
      no_insertions / (sm.length + 0.0)
    end

    ##
    # Returns the percentage of consesnsus residues from the ma
    # that are in the prediction
    # Params:
    # +prediction_raw+: +String+ corresponding to the prediction sequence
    # +consensus+: +String+ corresponding to the statistical model
    # Output:
    # +Fixnum+ with the score
    def consensus_validation(prediction_raw, consensus)
      return 1 if prediction_raw.length != consensus.length
      # no of conserved residues among the hits
      no_conserved_residues = consensus.length - consensus.scan(/[\?-]/).length

      return 1 if no_conserved_residues.zero?

      # no of conserved residues from the hita that appear in the prediction
      no_conserved_pred = consensus.split(//).each_index.count { |j| consensus[j] != '-' && consensus[j] != '?' && consensus[j] == prediction_raw[j] }

      no_conserved_pred / (no_conserved_residues + 0.0)
    end

    ##
    # Builds a statistical model from
    # a set of multiple aligned sequences
    # based on PSSM (Position Specific Matrix)
    # Params:
    # +ma+: array of +String+s, corresponding to the multiple aligned sequences
    # +threshold+: percentage of genes that are considered in statistical model
    # Output:
    # +String+ representing the statistical model
    # +Array+ with the maximum frequeny of the majoritary residue for each
    # position
    def get_sm_pssm(ma, threshold = 0.7)
      sm = ''
      freq = []
      (0..ma[0].length - 1).each do |i|
        freqs = Hash.new(0)
        ma.map { |seq| seq[i] }.each { |res| freqs[res] += 1 }
        # get the residue with the highest frequency
        max_freq = freqs.map { |_res, n| n }.max
        residue = freqs.map { |res, n| n == max_freq ? res : [] }.flatten[0]

        freq << residue == '-' ? 0 : (max_freq / ma.length.to_f)
        sm += (max_freq / ma.length.to_f) >= threshold ? residue : '?'
      end
      [sm, freq]
    end

    ##
    # Remove isolated residues inside long gaps
    # from a given sequence
    # Params:
    # +seq+:+String+: sequence of residues
    # +len+:+Fixnum+: number of isolated residues to be removed
    # Output:
    # +String+: the new sequence
    def remove_isolated_residues(seq, len = 2)
      gap_starts = seq.to_enum(:scan, /(-\w{1,#{len}}-)/i).map { |_m| $`.size + 1 }
      # remove isolated residues
      gap_starts.each do |i|
        (i..i + len - 1).each { |j| seq[j] = '-' if isalpha(seq[j]) }
      end
      # remove isolated gaps
      res_starts = seq.to_enum(:scan, /([?\w]-{1,2}[?\w])/i).map { |_m| $`.size + 1 }
      res_starts.each do |i|
        (i..i + len - 1).each { |j| seq[j] = '?' if seq[j] == '-' }
      end
      seq
    end

    ##
    # Returns true if the string contains only letters
    # and false otherwise
    def isalpha(str)
      !str.match(/[^A-Za-z]/)
    end

    ##
    # converts an array of integers into array of ranges
    def array_to_ranges(ar)
      prev = ar[0]

      ranges = ar.slice_before do |e|
        prev2 = prev
        prev = e
        prev2 + 1 != e
      end.map { |a| a[0]..a[-1] }

      ranges
    end

    # Generates a json file cotaining data used for plotting
    # lines for multiple hits alignment, prediction and statistical model
    # Params:
    # +freq+: +String+ residue frequency from the statistical model
    # +output+: plot_path of the json file
    # +ma+: +String+ array with the multiple alignmened hits and prediction
    def plot_alignment(freq, ma = @multiple_alignment)
      # get indeces of consensus in the multiple alignment
      consensus = get_consensus(ma[0..ma.length - 2])
      consensus_idxs = consensus.split(//).each_index.select { |j| isalpha(consensus[j]) }
      consensus_ranges = array_to_ranges(consensus_idxs)

      consensus_all = get_consensus(ma)
      consensus_all_idxs = consensus_all.split(//).each_index.select { |j| isalpha(consensus_all[j]) }
      consensus_all_ranges = array_to_ranges(consensus_all_idxs)

      match_alignment = ma[0..ma.length - 2].each_with_index.map { |seq, _j| seq.split(//).each_index.select { |j| isalpha(seq[j]) } }
      match_alignment_ranges = match_alignment.map { |e| array_to_ranges(e) }

      query_alignment = ma[ma.length - 1].split(//).each_index.select { |j| isalpha(ma[ma.length - 1][j]) }
      query_alignment_ranges = array_to_ranges(query_alignment)

      len = ma[0].length

      # plot statistical model
      data = freq.each_with_index.map { |h, j| { 'y' => ma.length, 'start' => j, 'stop' => j + 1, 'color' => 'orange', 'height' => h } } +
             # hits
             match_alignment_ranges.each_with_index.map { |ranges, j| ranges.map { |range| { 'y' => ma.length - j - 1, 'start' => range.first, 'stop' => range.last, 'color' => 'red', 'height' => -1 } } }.flatten +
             ma[0..ma.length - 2].each_with_index.map { |_seq, j| consensus_ranges.map { |range| { 'y' => j + 1, 'start' => range.first, 'stop' => range.last, 'color' => 'yellow', 'height' => -1 } } }.flatten +
             # plot prediction
             [{ 'y' => 0, 'start' => 0, 'stop' => len, 'color' => 'gray', 'height' => -1 }] +
             query_alignment_ranges.map { |range| { 'y' => 0, 'start' => range.first, 'stop' => range.last, 'color' => 'red', 'height' => -1 } }.flatten +

             # plot consensus
             consensus_all_ranges.map { |range| { 'y' => 0, 'start' => range.first, 'stop' => range.last, 'color' => 'yellow', 'height' => -1 } }.flatten

      y_axis_values = 'Prediction'
      (1..ma.length - 1).each { |i| y_axis_values << ", hit #{i}" }

      y_axis_values << ', Statistical Model'

      Plot.new(data,
               :align,
               'Missing/Extra sequences Validation: Multiple Align. &' \
               'Statistical model of hits',
               'Aligned Hit Sequence, red; Conserved Region, Yellow',
               'Offset in the Alignment',
               '',
               ma.length + 1,
               y_axis_values)
    end
  end
end
