(ns clojure-hadoop.examples.julia
  (:require [clojure-hadoop.gen :as gen]
            [clojure-hadoop.imports :as imp]
            [clojure.string :as str])
  (:import (java.util StringTokenizer)
           (org.apache.hadoop.util Tool))
  (:use clojure.test))

(def zoom (atom 1))
(def depth (atom 300))

(imp/import-conf)
(imp/import-fs)
(imp/import-io)
(imp/import-mapreduce)
(imp/import-mapreduce-lib)

(gen/gen-job-classes)
(gen/gen-main-method)

(defn cmod-opt [^double r0 ^double i0]
  (Math/sqrt (+ (Math/pow r0 2.0) (Math/pow i0 2.0))))
(defn c+-opt [^double r0 ^double i0 ^double r1 ^double i1]
  [(+ r0 r1) (+ i0 i1)])
(defn c*-opt [^double r0 ^double i0 ^double r1 ^double i1]
  [(- (* r0 r1) (* i0 i1)) (+ (* r0 i1) (* i0 r1))])
(defn cmod-opt [^double r0 ^double i0]
  (Math/sqrt (+ (Math/pow r0 2.0) (Math/pow i0 2.0))))

(defn normalize-coords [x y r-min x-step y-step]
  [(* @zoom (+ r-min (* x x-step))) (* @zoom (+ r-min (* y y-step)))])

(defn calculate-r-opt [^double r0 ^double i0]
  (/ (inc (Math/sqrt (inc (* 4.0 (cmod-opt r0 i0))))) 2.0))

(defn sq-poly-iteration-opt [^double r0 ^double i0 [^double rc ^double ic ^long max-itrs ^double radius]]
  (loop [i 0 zs [[r0 i0]]]
    (let [[r i] (last zs)
          [rr ii] (c*-opt r i r i)
          [rn in] (c+-opt rr ii rc ic)
          dis (cmod-opt rn in)]
      (if (or (> dis radius) (>= i max-itrs))
        zs
        (recur (inc i) (conj zs [rn in]))))))

(defn for-each-pixel-opt [[^double r0 ^double i0] [max-itrs radius r-min] [^double x-step ^double y-step] [^double j ^double i]]
  (let [[x y] (normalize-coords j i r-min x-step y-step)
        zitrs (sq-poly-iteration-opt x y [r0 i0 max-itrs radius])
        idx (count zitrs)]
    (when (> idx @depth)
      (reset! depth idx))
    idx))

(defn julia-subrect-opt [[^long start-x ^long start-y ^long end-x ^long end-y] [^double rc ^double ic] total-width total-height max-itrs ]
  (let [radius (calculate-r-opt rc ic)
        r-min (- radius)
        r-max radius
        x-step (/ (Math/abs (- r-max r-min)) total-width)
        y-step (/ (Math/abs (- r-max r-min)) total-height)]
   (loop [y start-y
          grid []]
     (cond
      (>= y end-y) grid
      :else (recur 
             (inc y) 
             (conj grid 
                   (loop [x start-x
                          row []]
                     (cond 
                      (>= x end-x) row
                      :else (recur (inc x) 
                                   (conj row 
                                         [x y (for-each-pixel-opt [rc ic] [max-itrs radius r-min] [x-step y-step] [x y])]))))))))))

; key: [x y]
; val: # itrs
(defn mapper-map
  [this key value ^MapContext context]
  "value will be: start-x start-y width height total-width total-height cr ci depth x-offset y-offset"
  (let [[start-x
         start-y
         width
         height
         total-width
         total-height
         cr
         ci
         depth-level
         zoom-level
         x-offset
         y-offset] (map read-string (str/split (str value) #" "))
         end-x (+ start-x width)
         end-y (+ start-y height)
         grid (julia-subrect-opt [start-x start-y end-x end-y] [cr ci] width height depth-level)]    (reset! zoom zoom-level)
    (reset! depth depth-level)
    (doseq [[[x y itrs]] grid]
      (.write context (proxy [ArrayWritable] [IntWritable (into-array IntWritable [(IntWritable. x) (IntWritable. y)])]) (LongWritable. itrs)))))

(defn reducer-reduce
  [this key values ^ReduceContext context]
  (println key))

(defn tool-run
  [^Tool this args]
  (doto (Job.)
        (.setJarByClass (.getClass this))
        (.setJobName "julia")
        (.setOutputKeyClass Text)
        (.setOutputValueClass LongWritable)
        (.setMapperClass (Class/forName "clojure_hadoop.examples.julia_mapper"))
        (.setReducerClass (Class/forName "clojure_hadoop.examples.julia_reducer"))
        (.setInputFormatClass TextInputFormat)
        (.setOutputFormatClass TextOutputFormat)
        (FileInputFormat/setInputPaths (first args))
        (FileOutputFormat/setOutputPath (Path. (second args)))
        (.waitForCompletion true))
  0)

(deftest test-julia
  (.delete (FileSystem/get (Configuration.)) (Path. "tmp/out1") true)
  (is (tool-run (clojure_hadoop.job.) ["test-resources/julia.txt" "tmp/out1"])))
