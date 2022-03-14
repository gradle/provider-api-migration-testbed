#!/bin/sh

local work_dir=$PWD

cd gradle-next
./gradlew install -Pgradle_installPath=$(PWD)/../gradle-next-install

cd $work_dir
