require "rgeo"

module RubyPager
  class Coord

    attr_reader :id
    def initialize(ex_index,ex_data)
      @data = ex_data
      @id = ex_index
      load_coords()
    end
    def id=(ex_id)
      raise(StandardError, "Got passed a negative value to update the x coord") if ex_id.to_i < 0
      @id=ex_id.to_i
    end

    def point
      @point.clone
    end

    def point=(ex_point)
      raise(ArgumentError, "Got passed a non point object") if ex_point.class != RGeo::Geos::CAPIPointImpl
      raise(StandardError, "Got passed a point with a negative value to update the x coord") if ex_point.x < 0
      raise(StandardError, "Got passed a point with a negative value to update the y coord") if ex_point.y < 0
      @point=ex_point
    end

    def x
      @point.x.to_i
    end

    def x=(x_coord)
      raise(StandardError, "Got passed a negative value to update the x coord") if x_coord.to_i < 0
      @point=RGeo::Cartesian.factory.point(x_coord.to_i,@point.y)
    end

    def y
      @point.y.to_i
    end

    def y=(y_coord)
      raise(StandardError, "Got passed a negative value to update the y coord") if y_coord.to_i < 0
      @point=RGeo::Cartesian.factory.point(@point.x,y_coord.to_i)
    end

    def get_consolidated_data
      consolidate_data()
      return @data
    end

    def vertical_noise(ex_std_dev)
      noise_generator=GaussianNoise.new(@y,ex_std_dev)
      @point=RGeo::Cartesian.factory.point(@point.x,noise_generator.rand.to_i)
    end

    private

    def load_coords()
      separate = @data.split(",")
      raise(StandardError,"Got passed coord data that doesn't have exactly two dimensions")if separate.size !=2
      @point=RGeo::Cartesian.factory.point(separate[0].to_i,separate[1].to_i)
    end

    def consolidate_data
      @data="#{@point.x.to_i},#{@point.y.to_i}"
    end

  end
end