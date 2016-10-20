#!/bin/bash

DIR=`dirname $0`

RUNDIR=$DIR/.run
mkdir -p $RUNDIR

LOGDIR=$DIR/.logs
mkdir -p $LOGDIR

DATADIR=$DIR/.mongo
mkdir -p $DATADIR

MONGO_VERSION=3.2.3
export PATH=$PATH:$HOME/Tools/mongodb-osx-x86_64-$MONGO_VERSION/bin

if [ -e $RUNDIR/mongo.pid ]; then
    pid=$(cat $RUNDIR/mongo.pid)
    if [ -n "$(ps -ef | awk -v pid=$pid '$2==pid { print $2 }')" ]; then
        echo "Mongo is running."
        exit 0
    fi
fi

which mongod 2>&1 > /dev/null
if [ $? -ne 0 ]; then
    echo "Unable to locate 'mongod'. Make sure the mongo bin path is mapped to the environment."
    exit 1
fi

nohup mongod --dbpath $DATADIR > $LOGDIR/mongo.log 2>&1 &
echo $! > $RUNDIR/mongo.pid

echo "Mongo is running in the background. Use 'stop_mongo.sh' to stop it."
