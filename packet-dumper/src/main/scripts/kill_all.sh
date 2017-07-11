#!/usr/bin/env bash

ps -ef | grep packet-dumper | grep java | awk '{ print $2}' | xargs kill -9