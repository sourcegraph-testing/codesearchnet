module ExportTo
  class Base < Struct.new(:records)
    class_attribute :head_titles
    class_attribute :body_keys
    class_attribute :body_column_proc
    class_attribute :presenter_klass
    class_attribute :join_relation
    class_attribute :each_proc, :each_method
    class_attribute :xlsx_file_path, :xlsx_file_name

    # 預設 presenter
    self.presenter_klass = Presenter
    self.each_method = :each
    self.xlsx_file_path = '/tmp'
    self.xlsx_file_name = '/file'

    attr_accessor :object

    def to_csv
      CSV.generate do |csv|
        rows! do |columns, model, x|
          csv << columns
        end
      end
    end

    # 新版 Excel
    def to_xlsx(file_path=nil, file_name=nil)
      file_path ||= self.class.xlsx_file_path
      file_name ||= self.class.xlsx_file_name
      path = to_xlsx_file(file_path, file_name)
      # TODO: 讀取檔案回傳
      File.open(path, 'rb') { |f| f.read }
    end

    # 新版 Excel (outpuut: path)
    def to_xlsx_file(file_path="tmp", file_name="export")
      path = "#{file_path}/#{file_name}_#{Time.now.to_i}_#{SecureRandom.hex}.xlsx"
      workbook = FastExcel.open(path, constant_memory: true)
      worksheet = workbook.add_worksheet("Default")

      bold = workbook.bold_cell_format
      worksheet.set_row(0, FastExcel::DEF_COL_WIDTH, bold)

      rows! do |columns, model, x|
        worksheet.write_row(x, columns)
      end

      workbook.close
      path
    end

    # 舊版 Excel
    def to_xls
      book = Spreadsheet::Workbook.new
      sheet = book.create_worksheet

      rows! do |columns, model, x|
        sheet.row(x).concat(columns)
      end

      spreadsheet = StringIO.new
      book.write(spreadsheet)
      spreadsheet.string
    end

    protected

    def rows
      data = []
      rows! do |columns, model, x|
        data.push(columns)
      end
      data
    end

    def rows!
      i = 0

      yield(self.class.head_titles, nil, i)

      join_relation = self.class.join_relation

      each_models do |record, x|
        run_records = if join_relation.present? && record.send(join_relation).present?
          record.send(join_relation)
        else
          [ record ]
        end

        # 指定目前 order 讓 subclass 可以 override
        run_records.each_with_index do |run_record, y|
          i = i + 1
          object = if run_record != record
            self.class.presenter_klass.new(record, run_record, x, y)
          else
            self.class.presenter_klass.new(run_record, nil, x, y)
          end

          each_proc.call(object) if self.class.each_proc.present?

          columns = fetch_columns!(object)

          yield(columns, run_record, i)
        end
      end
    end

    def each_models(&block)
      case self.class.each_method
      when :each
        records.each.with_index(&block)
      when :find_in_batches
        find_in_batches(&block)
      end
    end

    def find_in_batches
      i = 0
      records.find_in_batches do |group|
        group.each do |model|
          yield(model, i)
          i = i + 1
        end
      end
    end

    class << self

      protected

      def set(title, key, &block)
        self.head_titles ||= []
        self.body_keys ||= []
        self.body_column_proc ||= []

        self.head_titles.push(title)
        self.body_keys.push(key)
        self.body_column_proc.push(block)
      end

      def presenter(&block)
        # 依序尋找繼承關係的 presenter
        parent_klass = ancestors.find { |klass|
          klass.respond_to?(:presenter_klass) && !klass.presenter_klass.nil?
        }
        # 建立當下的 presenter 內容
        klass = Class.new(parent_klass.presenter_klass, &block)

        # 註冊成為 presenter class
        const_set(:Presenter, klass)
        self.presenter_klass = const_get(:Presenter)
      end

      def joins(relation)
        self.join_relation = relation
      end

      def each_with(&block)
        self.each_proc = block
      end
    end

    private

    def fetch_columns!(object)
      self.class.body_keys.map.with_index do |key, i|
        data = case key
        when String
          key
        when Symbol
          (object.send(key) || "")
        end

        column_proc = self.class.body_column_proc[i]
        data = self.class.body_column_proc[i].call(data) if column_proc.present?
        data
      end
    end
  end
end
