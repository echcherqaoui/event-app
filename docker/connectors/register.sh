#!/bin/bash

set -a
source .env
set +a

envsubst '${PG_USER} ${PG_PASSWORD} ${PG_PORT} ${BOOKING_DB}' < docker/connectors/booking-connector.json | \
  curl -X POST http://localhost:${KF_CONNECT_PORT}/connectors -H "Content-Type: application/json" -d @-

envsubst '${PG_USER} ${PG_PASSWORD} ${PG_PORT} ${PAYMENT_DB}' < docker/connectors/payment-connector.json | \
  curl -X POST http://localhost:${KF_CONNECT_PORT}/connectors -H "Content-Type: application/json" -d @-