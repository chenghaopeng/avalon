#!/bin/sh
set -e
DIR="$( cd "$( dirname "$0"  )" && pwd  )"
cd $DIR

docker run --rm -it -v $PWD:/code -w /code node:14.17.6-alpine sh -c "yarn config set registry https://registry.npm.taobao.org && yarn && yarn build"
# yarn && yarn build
docker build -t registry.cn-hangzhou.aliyuncs.com/chaop-public/avalon-frontend .
docker push registry.cn-hangzhou.aliyuncs.com/chaop-public/avalon-frontend
