(ns clojure-hadoop.examples.julia
  (:require [clojure-hadoop.gen :as gen]
            [clojure-hadoop.imports :as imp]
            [java.string :as str])
  (:import (java.util StringTokenizer)
           (org.apache.hadoop.util Tool))
  (:use clojure.test))

(imp/import-conf)
(imp/import-fs)
(imp/import-io)
(imp/import-mapreduce)
(imp/import-mapreduce-lib)

(gen/gen-job-classes)
(gen/gen-main-method)

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
    (when (> idx @impure/max-idx)
      (reset! impure/max-idx idx))
    [j i idx]))

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
                                         (for-each-pixel-opt [rc ic] [max-itrs radius r-min] [x-step y-step] [x y])))))))))))

(gen-class
 :name IntArrayWritable
 :extends ArrayWritable
 :constructors {[]}
 :init (fn [] [[IntWritable] nil]))

; key: [x y]
; val: # itrs
(defn mapper-map
  [this key value ^MapContext context]
  "value will be: width height cr ci depth x-offset y-offset"
  (let [tokens (str/split (str value) #" ")
        width (first tokens)
        height (second tokens)
        cr (nth tokens 2)
        ci (nth tokens 3)
        depth (nth tokens 4)
        zoom (nth tokens 5)
        xoffset (nth tokens 6)
        yoffset (nth tokens 7)]
    (doseq [[x y]])
    (.write context (IntArrayWritable. [x y]) (LongWritable. 1)))

