# Makefile for Docker Compose commands

# Variables
COMPOSE_FILE=docker-compose.yml

# Commands
up:
	docker-compose -f $(COMPOSE_FILE) up -d

down:
	docker-compose -f $(COMPOSE_FILE) down

restart:
	docker-compose -f $(COMPOSE_FILE) down && docker-compose -f $(COMPOSE_FILE) up -d

logs:
	docker-compose -f $(COMPOSE_FILE) logs -f

ps:
	docker-compose -f $(COMPOSE_FILE) ps

build:
	docker-compose -f $(COMPOSE_FILE) build

stop:
	docker-compose -f $(COMPOSE_FILE) stop

start:
	docker-compose -f $(COMPOSE_FILE) start

clean:
	docker-compose -f $(COMPOSE_FILE) down -v --remove-orphans

exec:
	docker exec -it postgres-container bash