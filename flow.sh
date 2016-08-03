#!/bin/bash

# add changes to git
git add --all
git commit -m $1
git push

# build without cache
docker build -t johnmarinelli/clojure-hadoop .
docker run -it johnmarinelli/clojure-hadoop bash
