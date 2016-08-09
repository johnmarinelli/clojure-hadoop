#!/bin/bash 

echo "Cloning my fork of clojure-hadoop..."
git clone https://github.com/johnmarinelli/clojure-hadoop
echo "Compiling"
export LEIN_ROOT=$USER
cd clojure-hadoop 
lein deps 
lein uberjar 
mv target/examples.jar . 
java -cp examples.jar clojure_hadoop.examples.julia julia/julia.txt julia/julia2.txt julia/julia3.txt julia/julia4.txt outj
