(ns julia.int-array-writable
  "IntArrayWritable class for using [x y] as hadoop keys"
  (:require [clojure-hadoop.gen])
  
  (gen-class
   :name IntArrayWritable
   :extends ArrayWritable
   :constructors {[] [IntWritable]}
   :init (fn [] [[IntWritable] nil])
   :main false))
