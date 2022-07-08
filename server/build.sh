#!/bin/sh
set -e
DIR="$( cd "$( dirname "$0"  )" && pwd  )"
cd $DIR

docker run --rm -it -v ~/.m2:/root/.m2 -v $PWD:/code -w /code maven:3.8.6-openjdk-11-slim mvn -DskipTests package
docker build -t registry.cn-hangzhou.aliyuncs.com/chaop-public/avalon-backend .
docker push registry.cn-hangzhou.aliyuncs.com/chaop-public/avalon-backend
