include .env
export

PROJECT_NAME = eventapp-parent
INFRA_FILE   = -f docker/infra.yml
APP_FILE     = -f docker/app.yml
DEV_FILE     = -f docker/dev-tools.yml
ENV_FILE     = --env-file .env

# ─── Infrastructure ───────────────────────────────────────────
up-infra:
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) up -d --remove-orphans

# ─── Application ──────────────────────────────────────────────
up-app:
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) $(APP_FILE) up -d --remove-orphans

# ─── Dev Tools ────────────────────────────────────────────────
up-dev-tools:
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) $(DEV_FILE) up -d --remove-orphans

# ─── Full Stack ───────────────────────────────────────────────
up-dev:
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) $(APP_FILE) $(DEV_FILE) up -d --remove-orphans

# ─── Rebuild & Start ──────────────────────────────────────────
rebuild-all:
	./mvnw clean install -DskipTests
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) $(APP_FILE) up -d --build --remove-orphans

rebuild-service:
	./mvnw clean install -pl services/$(SERVICE) -am -DskipTests
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) $(APP_FILE) up -d --build --remove-orphans $(SERVICE)

# ─── Schema Registry ──────────────────────────────────────────
register-schemas:
	./mvnw -pl common/eventapp-common-contracts \
		-P register-schemas \
		io.confluent:kafka-schema-registry-maven-plugin:$(confluent.version):register \
		-Dschema.registry.url=$(SC_REGISTRY_HOST_URL)

# ─── Down ─────────────────────────────────────────────────────
down:
	docker compose -p $(PROJECT_NAME) $(ENV_FILE) $(INFRA_FILE) $(APP_FILE) $(DEV_FILE) down --remove-orphans