#!/bin/bash

printf "Loading all operators...\n"
nohup java -jar scep-operator-op1-v3.jar &
printf "Operator 1 is up, loading operator 2...\n"
nohup java -jar scep-operator-op2-v3.jar &
printf "Operator 2 is up, loading operator 3...\n"
nohup java -jar scep-operator-op3-v3.jar &
printf "Operator 3 is up, loading operator 4...\n"
nohup java -jar scep-operator-op4-v3.jar &
printf "Operator 4 is up, loading operator 5...\n"
nohup java -jar scep-operator-op5-v3.jar &
printf "Operator 5 is up, loading operator 6...\n"
nohup java -jar scep-operator-op6-v3.jar &
printf "All operators are up!\n"

