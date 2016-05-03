module Embulk
  def self.create_example(path)
    require 'fileutils'
    require 'zlib'

    puts "  Creating #{path}/"
    FileUtils.mkdir_p File.join(path, 'csv')
    puts "  Creating #{path}/csv/"

    puts "  Creating #{path}/csv/sample_01.csv.gz"
    Zlib::GzipWriter.open(File.join(path, 'csv', 'sample_01.csv.gz')) do |f|
      f.write <<EOF
id,account,time,purchase,comment
1,32864,2015-01-27 19:23:49,20150127,embulk
2,14824,2015-01-27 19:01:23,20150127,embulk jruby
3,27559,2015-01-28 02:20:02,20150128,"Embulk ""csv"" parser plugin"
4,11270,2015-01-29 11:54:36,20150129,NULL

EOF
    end

    puts "  Creating #{path}/seed.yml"
    File.open(File.join(path, 'seed.yml'), 'w') do |f|
      f.write <<EOF
in:
  type: file
  path_prefix: "#{File.expand_path File.join(path, 'csv', 'sample_')}"
out:
  type: stdout
EOF
    end
  end
end
