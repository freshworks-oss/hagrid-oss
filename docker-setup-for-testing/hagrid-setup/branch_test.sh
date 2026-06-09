#!/bin/sh 

set -e


cwd=$(pwd)
branch="main"

if [ -n "$1" ]; then 
  echo "Will run performance test on this branch $1"
  branch=$1
else
    echo "Will run performance test on main branch. If you want to run performance test on specific branch, please pass it as first argument"

fi 

echo "Making progress forward"

# Generate a timestamp (Format: YYYY-MM-DD_HH-MM-SS)
timestamp=$(date +'%Y-%m-%d_%H-%M-%S')

# Create the directory
mkdir -p "$timestamp"

# Navigate into it or reference it later
cd "$timestamp"

echo "Cloning Hagrid OSS git repository in $cwd/$timestamp"

git clone https://github.com/freshworks-oss/hagrid-oss.git .


echo "Checking out $branch branch from hagrid-oss"
git checkout $branch


echo "Checking out Hagrid version of the current branch"

echo "building test cases for ${SPRING_PROFILES_ACTIVE} profile"

if [ -n "$SPRING_PROFILES_ACTIVE" ]; then 
  echo "Running maven test cases with profile $SPRING_PROFILES_ACTIVE"
  mvn clean test -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}
else
  echo "Running maven test cases without profile. Running default unit test cases with profile mentioned in pom.xml"
  mvn clean test

fi


