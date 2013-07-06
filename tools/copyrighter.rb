#!/usr/bin/env ruby

require 'find'
require 'fileutils'

COPYRIGHT_STATEMENT = <<END
Copyright 2013 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
END


PRUNE_DIRS = [ 'target', 'tmp', '.git' ]

def comment_wrap(text, comment_line, comment_begin=nil, comment_end=nil)
  wrapped = ""
  wrapped << "#{comment_begin}\n" if comment_begin
  text.split("\n").each do |line|
    wrapped << "#{comment_line} #{line}\n"
  end
  wrapped << "#{comment_end}\n" if comment_end
  wrapped
end

LANGUAGE_COMMENT_DELIMS = {
  :java => [ ' *', '/*', ' */' ],
  :xml  => [ '    ', '<!--', '-->' ],
  #:ruby => [ '#' ],
  :clojure => [ ';;' ]
}

COPYRIGHT_STATEMENTS = {
  :java    => comment_wrap( COPYRIGHT_STATEMENT, *LANGUAGE_COMMENT_DELIMS[:java] ),
  :xml     => comment_wrap( COPYRIGHT_STATEMENT, *LANGUAGE_COMMENT_DELIMS[:xml]  ),
  #:ruby    => comment_wrap( COPYRIGHT_STATEMENT, *LANGUAGE_COMMENT_DELIMS[:ruby] ),
  :clojure => comment_wrap( COPYRIGHT_STATEMENT, *LANGUAGE_COMMENT_DELIMS[:clojure] )
}

def header(lang)
  COPYRIGHT_STATEMENTS[lang]
end

def project_dirs
  dirs = []
  Find.find( '.' ) do |path|
    basename = File.basename( path )
    Find.prune if ( PRUNE_DIRS.include?( basename ) )
    dirs << File.dirname( path ) if ( basename == 'pom.xml' )
  end
  dirs
end

def copywrite_dir(dir, lang, glob)
  $stderr.puts("Inspecting #{glob}")
  Dir[ File.join( dir, glob ) ].each do |file|
    copywrite_file( file, lang )
  end
end

def copywrite_file(file, lang)
  bak_file = file + '.bak'
  FileUtils.cp( file, bak_file ) 
  File.open( bak_file, 'r' ) do |input_file|
    #output_file = $stdout
    File.open( file, 'w' ) do |output_file|
      skip_header_comment(input_file, lang)
      output_file.puts header(lang)
      output_file.puts "\n"
      input_file.each_line do |line|
        output_file.puts line
      end
    end
  end
  FileUtils.rm( bak_file )
end

def skip_header_comment(input, lang)
  delims = LANGUAGE_COMMENT_DELIMS[lang]
  skip_blank_lines( input )
  if ( delims.size == 1 )
    skip_simple_header_comment( input, *delims )
  else
    skip_block_header_comment( input, *delims )
  end
  skip_blank_lines( input )
end

def skip_blank_lines(input)
  while ( ! input.eof? )
    pos = input.pos
    line = input.readline
    if ( line.strip != '' )
      input.seek( pos )
      input.seek( pos )
      return
    end
  end
end

def skip_simple_header_comment(input, comment_line)
  while ( true )
    pos = input.pos
    line = input.readline
    next if ( line.strip =~ /^#{Regexp.escape(comment_line.strip)}/ )
    input.seek( pos ) 
    return
  end 
end

def skip_block_header_comment(input, comment_line, comment_begin, comment_end )
  state = :begin
  while ( true )
    pos = input.pos
    line = input.readline
    case ( state )
      when :begin
        if ( line.strip =~ /^#{Regexp.escape(comment_begin.strip)}/ )
          return if ( line.strip =~ /#{Regexp.escape(comment_end.strip)}$/ )
          state = :skip
          next
        else
          input.seek( pos )
          return
        end
      when :skip
        if ( line.strip =~ /#{Regexp.escape(comment_end.strip)}$/ )
          return
        end
    end
  end
  return
end

project_dirs.each do |dir|
  $stderr.puts "Copywriting: #{dir}"
  copywrite_dir( dir, :java, "src/*/java/**/*.java" )
  copywrite_dir( dir, :clojure, "src/*/clojure/**/*.clj" )
  copywrite_dir( dir, :clojure, "src/test/resources/**/*.clj" )
  copywrite_dir( dir, :clojure, "examples/**/*.clj" )
end




