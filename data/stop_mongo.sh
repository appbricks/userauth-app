#!/bin/bash

DIR=`dirname $0`
RUNDIR=$DIR/.run

if [ -e $RUNDIR/mongo.pid ]
then
  kill -15 $(cat $RUNDIR/mongo.pid)
  rm $RUNDIR/mongo.pid
  echo "Mongod stopped."
fi
