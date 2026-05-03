#!/bin/bash

# required: docker
if ! command -v docker &>/dev/null; then
	echo "Error: Docker is not installed or not in PATH."
	echo "See <https://docs.docker.com/engine/install/> to install Docker."
	exit 1
fi

# required: mvn
if ! command -v mvn &>/dev/null; then
	echo "Error: Maven is not installed or not in PATH."
	echo "See <https://maven.apache.org/install.html> to install Maven."
	exit 1
fi

# required: java (version 25+)
if ! command -v java &>/dev/null; then
	echo "Error: Java is not installed or not in PATH."
	echo "This project expects Java 25"
	echo "We suggest using SDKMAN to manage Java versions: <https://sdkman.io/install>"
	exit 1
fi

echo "Setting up database..."
# check if docker service is running
if ! systemctl is-active --quiet docker; then
	echo "Error: Docker service is not running."
	echo "Please start the Docker service and try again."
	echo "This is probably done using sudo systemctl start docker"
	exit 1
fi

# check if we already have the container running
if docker ps --filter "name=omiyage-db" --filter "status=running" | grep -q omiyage-db; then
	echo "Database container is already running."
else
	# check if we have a stopped container with the same name
	if docker ps -a --filter "name=omiyage-db" | grep -q omiyage-db; then
		echo "Starting existing database container..."
		docker start omiyage-db
	else
		echo "Creating and starting new database container..."
		docker run --name omiyage-db \
			-e POSTGRES_USER=omiyage \
			-e POSTGRES_PASSWORD=omiyage \
			-e POSTGRES_DB=omiyage \
			-p 5432:5432 -d postgres:latest
	fi
fi

echo "Waiting for database to be ready..."
until docker exec omiyage-db pg_isready -U omiyage; do
	sleep 5
done

echo "Database is ready. Starting backend server..."
DATABASE_URL=jdbc:postgresql://localhost:5432/omiyage \
	DATABASE_USERNAME=omiyage \
	DATABASE_PASSWORD=omiyage \
	SERVER_PORT=8080 mvn spring-boot:run
