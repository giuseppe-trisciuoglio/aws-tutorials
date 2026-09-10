# Configurazione
SHELL := /bin/bash
REGION ?= eu-west-1
# Su AWS CloudShell le credenziali dell'utente loggato sono automatiche
# (IAM Role della console). Non serve AWS_PROFILE: lasciamo vuoto per
# usare le credenziali di default di CloudShell.
AWS_PROFILE ?=
ACCOUNT_ID := $(shell aws sts get-caller-identity --query Account --output text $(if $(AWS_PROFILE),--profile $(AWS_PROFILE)))
# Devono corrispondere a SuffissoName e ApplicationTag nei parameters.json:
# il bucket creato da setup-buckets si chiama
# $(SUFFIX_NAME)-$(APPLICATION_NAME)-cf-templates-$(ENV).
SUFFIX_NAME = gt
APPLICATION_NAME = supplier-gateway
TEMPLATE_BUCKET = $(SUFFIX_NAME)-$(APPLICATION_NAME)-cf-templates-$(ENV)
ALLOWED_STEPS := setup-buckets setup-environment setup-database setup-amazonmq setup-secrets setup-cluster-ecs
STEP_NAME := null
STACK_NAME ?= $(APPLICATION_NAME)-$(STEP_NAME)-$(ENV)
ENV ?= dev
TEMPLATES_DIR = templates
MAIN_STACK_FILE = main-stack.yaml

# Colori per output
RED := \033[0;31m
GREEN := \033[0;32m
YELLOW := \033[1;33m
NC := \033[0m

# AWS CLI base command (con o senza profilo)
ifdef AWS_PROFILE
AWS_CMD = aws --profile $(AWS_PROFILE)
else
AWS_CMD = aws
endif

.PHONY: all clean validate upload deploy delete check-env check-aws create-bucket help list-profiles

# Target di default
all: help

# Mostra help
help:
	@echo "Utilizzo:"
	@echo "  make validate                    - Valida tutti i template CloudFormation"
	@echo "  make upload                      - Carica i template su S3"
	@echo "  make deploy STEP_NAME=<step>     - Deploy dello stack indicato"
	@echo "  make delete STEP_NAME=<step>     - Elimina lo stack indicato"
	@echo "  make list-profiles               - Lista i profili AWS disponibili"
	@echo ""
	@echo "Step disponibili (ordine di deploy):"
	@echo "  setup-buckets                    - bucket S3 per i template CF"
	@echo "  setup-environment                - VPC, subnets, NAT, VPC endpoints"
	@echo "  setup-database                   - RDS PostgreSQL 17"
	@echo "  setup-amazonmq                   - broker AmazonMQ RabbitMQ managed"
	@echo "  setup-secrets                    - Secrets Manager per i supplier"
	@echo "  setup-cluster-ecs                - cluster ECS, ALB, 4 servizi Fargate"
	@echo ""
	@echo "Variabili:"
	@echo "  REGION=<region>                  - Region AWS (default: eu-west-1)"
	@echo "  AWS_PROFILE=<profile>            - Profilo AWS (vuoto su CloudShell)"
	@echo "  ENV=<env>                        - Ambiente (default: dev)"
	@echo "  STACK_NAME=<name>                - Nome dello stack (default: auto)"

# Lista i profili AWS disponibili
list-profiles:
	@echo -e "$(GREEN)Profili AWS disponibili:$(NC)"
	@aws configure list-profiles 2>/dev/null | sed 's/^/  /' || echo "  (nessun profilo configurato)"

# Verifica che AWS CLI sia configurato
check-aws:
	@echo -e "$(GREEN)Verificando configurazione AWS...$(NC)"
	@$(AWS_CMD) sts get-caller-identity > /dev/null || \
		(echo -e "$(RED)AWS CLI non configurato correttamente. Verifica le credenziali o l'ambiente CloudShell.$(NC)" && exit 1)
	@echo -e "$(GREEN)Account: $$($(AWS_CMD) sts get-caller-identity --query Account --output text)$(NC)"
	@echo -e "$(GREEN)Region:  $(REGION)$(NC)"

# Verifica che STEP_NAME sia impostato
check-step:
ifeq ($(STEP_NAME),null)
	@echo -e "$(RED)STEP_NAME non è impostato. Uso: make deploy STEP_NAME=setup-environment$(NC)"
	@exit 1
endif
ifeq ($(filter $(STEP_NAME),$(ALLOWED_STEPS)),)
	@echo -e "$(RED)STEP_NAME non valido. Valori consentiti: $(ALLOWED_STEPS)$(NC)"
	@exit 1
endif
	@echo -e "$(GREEN)STEP_NAME: $(STEP_NAME)$(NC)"

# Verifica che i file necessari esistano
check-env: check-step
	@echo -e "$(GREEN)Verificando ambiente...$(NC)"
	@test -d $(TEMPLATES_DIR) || (echo -e "$(RED)Directory templates non trovata$(NC)" && exit 1)
	@test -f $(TEMPLATES_DIR)/$(STEP_NAME)/parameters.json || \
		(echo -e "$(RED)File parameters.json non trovato in $(TEMPLATES_DIR)/$(STEP_NAME)/$(NC)" \
		&& echo "Copia parameters.json.dist: cp $(TEMPLATES_DIR)/$(STEP_NAME)/parameters.json.dist $(TEMPLATES_DIR)/$(STEP_NAME)/parameters.json" \
		&& exit 1)
	@echo -e "$(GREEN)STACK_NAME:  $(STACK_NAME)$(NC)"
	@echo -e "$(GREEN)ENV:         $(ENV)$(NC)"
	@echo -e "$(GREEN)TEMPLATE_BUCKET (target): $(TEMPLATE_BUCKET)$(NC)"

# Valida i template CloudFormation
validate: check-aws
	@echo -e "$(GREEN)Validando i template CloudFormation...$(NC)"
	@for template in $(shell find $(TEMPLATES_DIR) -name "*.yaml"); do \
		echo -e "$(GREEN)  Validando $$template...$(NC)"; \
		$(AWS_CMD) cloudformation validate-template \
			--template-body file://$$template > /dev/null || exit 1; \
	done
	@echo -e "$(GREEN)Validazione completata con successo$(NC)"

# Crea il bucket S3 se non esiste
create-bucket: check-aws
	@echo -e "$(GREEN)Verificando bucket S3 $(TEMPLATE_BUCKET)...$(NC)"
	@if ! $(AWS_CMD) s3 ls "s3://$(TEMPLATE_BUCKET)" 2>&1 > /dev/null; then \
		echo -e "$(GREEN)Bucket non trovato. Creando bucket $(TEMPLATE_BUCKET)...$(NC)"; \
		echo -e "$(YELLOW)Nota: lo stack setup-buckets gestisce il bucket canonico.$(NC)"; \
		echo -e "$(YELLOW)Questo create-bucket è un fallback per casi non-standard.$(NC)"; \
		$(AWS_CMD) s3 mb s3://$(TEMPLATE_BUCKET) --region $(REGION); \
		$(AWS_CMD) s3api put-bucket-encryption \
			--bucket $(TEMPLATE_BUCKET) \
			--server-side-encryption-configuration \
			'{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}]}'; \
		$(AWS_CMD) s3api put-public-access-block \
			--bucket $(TEMPLATE_BUCKET) \
			--public-access-block-configuration \
			'{"BlockPublicAcls":true,"IgnorePublicAcls":true,"BlockPublicPolicy":true,"RestrictPublicBuckets":true}'; \
		$(AWS_CMD) s3api put-bucket-versioning \
			--bucket $(TEMPLATE_BUCKET) \
			--versioning-configuration Status=Enabled; \
	fi

# Carica i template su S3
upload: validate create-bucket
	@echo -e "$(GREEN)Caricando i template su S3...$(NC)"
	@$(AWS_CMD) s3 sync $(TEMPLATES_DIR) s3://$(TEMPLATE_BUCKET)/ \
		--exclude "*" --include "*.yaml" \
		--delete
	@echo -e "$(GREEN)Upload completato$(NC)"

# Funzione per attendere il completamento dello stack
define wait-for-stack
	@echo -e "$(GREEN)Attendendo completamento stack...$(NC)"
	@while true; do \
		status=$$($(AWS_CMD) cloudformation describe-stacks \
			--stack-name $(STACK_NAME) \
			--query 'Stacks[0].StackStatus' \
			--output text); \
		case $$status in \
			*COMPLETE) \
				echo -e "$(GREEN)Stack completato: $$status$(NC)"; \
				break ;; \
			*FAILED) \
				echo -e "$(RED)Stack fallito: $$status$(NC)"; \
				exit 1 ;; \
			*IN_PROGRESS) \
				echo -n "."; \
				sleep 15 ;; \
			*) \
				echo -e "$(RED)Stato stack non riconosciuto: $$status$(NC)"; \
				exit 1 ;; \
		esac \
	done
endef

# Deploy dello stack
deploy: upload
	@echo -e "$(GREEN)Deploy dello stack $(STACK_NAME)...$(NC)"
	@if $(AWS_CMD) cloudformation describe-stacks --stack-name $(STACK_NAME) 2>&1 > /dev/null; then \
		echo -e "$(YELLOW)Stack esistente: aggiornamento...$(NC)"; \
		$(AWS_CMD) cloudformation update-stack \
			--stack-name $(STACK_NAME) \
			--template-url https://s3.$(REGION).amazonaws.com/$(TEMPLATE_BUCKET)/$(STEP_NAME)/$(MAIN_STACK_FILE) \
			--parameters file://$(TEMPLATES_DIR)/$(STEP_NAME)/parameters.json \
			--capabilities CAPABILITY_NAMED_IAM \
			--region $(REGION) || \
		(error_code=$$?; if [ $$error_code -ne 255 ]; then exit $$error_code; fi); \
	else \
		echo -e "$(GREEN)Creando nuovo stack...$(NC)"; \
		$(AWS_CMD) cloudformation create-stack \
			--stack-name $(STACK_NAME) \
			--template-url https://s3.$(REGION).amazonaws.com/$(TEMPLATE_BUCKET)/$(STEP_NAME)/$(MAIN_STACK_FILE) \
			--parameters file://$(TEMPLATES_DIR)/$(STEP_NAME)/parameters.json \
			--capabilities CAPABILITY_NAMED_IAM \
			--region $(REGION); \
	fi
	$(call wait-for-stack)
	@echo -e "$(GREEN)Outputs dello stack:$(NC)"
	@$(AWS_CMD) cloudformation describe-stacks \
		--stack-name $(STACK_NAME) \
		--query 'Stacks[0].Outputs[].[OutputKey,OutputValue]' \
		--output table

# Elimina lo stack
delete: check-step
	@echo -e "$(YELLOW)Eliminando stack $(STACK_NAME)...$(NC)"
	@$(AWS_CMD) cloudformation delete-stack --stack-name $(STACK_NAME) --region $(REGION)
	$(call wait-for-stack)
	@echo -e "$(GREEN)Stack eliminato con successo$(NC)"

# Pulisce file temporanei
clean:
	@echo -e "$(GREEN)Pulizia file temporanei...$(NC)"
	@find . -name "*.log" -type f -delete
	@find . -name "*.bak" -type f -delete
	@find . -name "*~" -type f -delete