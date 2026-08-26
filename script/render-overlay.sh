#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TEMPLATE_DIR="$ROOT/kustomize/templates"
DEFAULT_TEMPLATE="$TEMPLATE_DIR/overlay.yaml.tpl"

PROD_DISPATCH_BASE_URL="https://vigoiks.service-now.com/api/now/table"
PROD_DISPATCH_TOKEN_URL="https://vigoiks.service-now.com/oauth_token.do"
TEST_DISPATCH_BASE_URL="https://vigoikstest.service-now.com/api/now/table"
TEST_DISPATCH_TOKEN_URL="https://vigoikstest.service-now.com/oauth_token.do"

choose_template() {
  local environment="$1"
  local candidate="$DEFAULT_TEMPLATE"

  if [[ -n "$environment" && "$environment" != "api" ]]; then
    candidate="$TEMPLATE_DIR/overlay-${environment}.yaml.tpl"
  fi

  if [[ -f "$candidate" ]]; then
    printf '%s' "$candidate"
  else
    printf '%s' "$DEFAULT_TEMPLATE"
  fi
}

overlay_config() {
  local namespace="$1"
  local environment="$2"

  ORG_NUMBER=""
  CHECK_SAKSANSVARLIG=""
  CHECK_SAKSBEHANDLER=""
  CHECK_EMAIL_DOMAIN=""
  DISPATCH_BASE_URL_OVERRIDE=""
  DISPATCH_TOKEN_URI_OVERRIDE=""

  case "$namespace" in
    afk-no)
      ORG_NUMBER=930580783
      ;;
    agderfk-no)
      ORG_NUMBER=921707134
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    bfk-no)
      ORG_NUMBER=930580260
      ;;
    ffk-no)
      ORG_NUMBER=830090282
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    fintlabs-no)
      ORG_NUMBER=999999999
      if [[ "$environment" == "beta" ]]; then
        CHECK_EMAIL_DOMAIN=false
        DISPATCH_BASE_URL_OVERRIDE="$PROD_DISPATCH_BASE_URL"
        DISPATCH_TOKEN_URI_OVERRIDE="$PROD_DISPATCH_TOKEN_URL"
      fi
      ;;
    innlandetfylke-no)
      ORG_NUMBER=920717152
      ;;
    mrfylke-no)
      ORG_NUMBER=944183779
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    nfk-no)
      ORG_NUMBER=964982953
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    ofk-no)
      ORG_NUMBER=930580694
      ;;
    rogfk-no)
      ORG_NUMBER=971045698
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    telemarkfylke-no)
      ORG_NUMBER=929882989
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    tromsfylke-no)
      ORG_NUMBER=930068128
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    trondelagfylke-no)
      ORG_NUMBER=817920632
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    vestfoldfylke-no)
      ORG_NUMBER=929882385
      CHECK_SAKSANSVARLIG=false
      CHECK_SAKSBEHANDLER=false
      ;;
    vlfk-no)
      ORG_NUMBER=821311632
      if [[ "$environment" == "beta" ]]; then
        CHECK_SAKSANSVARLIG=false
        CHECK_SAKSBEHANDLER=false
      fi
      ;;
    *)
      echo "Missing config for namespace '$namespace'" >&2
      exit 1
      ;;
  esac
}

set_dispatch_urls() {
  local environment="$1"

  local default_base="$TEST_DISPATCH_BASE_URL"
  local default_token="$TEST_DISPATCH_TOKEN_URL"

  if [[ "$environment" == "api" ]]; then
    default_base="$PROD_DISPATCH_BASE_URL"
    default_token="$PROD_DISPATCH_TOKEN_URL"
  fi

  DISPATCH_BASE_URL="${DISPATCH_BASE_URL_OVERRIDE:-$default_base}"
  DISPATCH_TOKEN_URI="${DISPATCH_TOKEN_URI_OVERRIDE:-$default_token}"
}

build_env_patches() {
  local patches=""

  if [[ -n "${CHECK_SAKSANSVARLIG:-}" ]]; then
    patches+="
      - op: add
        path: \"/spec/env/-\"
        value:
          name: novari.flyt.egrunnerverv.checkSaksansvarligEpost
          value: \"${CHECK_SAKSANSVARLIG}\""
  fi

  if [[ -n "${CHECK_SAKSBEHANDLER:-}" ]]; then
    patches+="
      - op: add
        path: \"/spec/env/-\"
        value:
          name: novari.flyt.egrunnerverv.checkSaksbehandler
          value: \"${CHECK_SAKSBEHANDLER}\""
  fi

  if [[ -n "${CHECK_EMAIL_DOMAIN:-}" ]]; then
    patches+="
      - op: add
        path: \"/spec/env/-\"
        value:
          name: novari.flyt.egrunnerverv.checkEmailDomain
          value: \"${CHECK_EMAIL_DOMAIN}\""
  fi

  ENV_PATCHES="$patches"
}

build_dispatch_patches() {
  DISPATCH_PATCHES=""

  if [[ "$DISPATCH_BASE_URL" != "$TEST_DISPATCH_BASE_URL" || "$DISPATCH_TOKEN_URI" != "$TEST_DISPATCH_TOKEN_URL" ]]; then
    DISPATCH_PATCHES+="
      - op: add
        path: \"/spec/env/-\"
        value:
          name: novari.flyt.egrunnerverv.dispatch.base-url
          value: \"${DISPATCH_BASE_URL}\"
      - op: add
        path: \"/spec/env/-\"
        value:
          name: novari.flyt.egrunnerverv.dispatch.token-uri
          value: \"${DISPATCH_TOKEN_URI}\""
  fi
}

while IFS= read -r file; do
  rel="${file#"$ROOT/kustomize/overlays/"}"
  dir="$(dirname "$rel")"

  namespace="${dir%%/*}"
  env_path="${dir#*/}"
  environment="$env_path"
  if [[ -z "$env_path" || "$env_path" == "$namespace" ]]; then
    environment="api"
  fi

  overlay_config "$namespace" "$environment"
  set_dispatch_urls "$environment"
  build_env_patches
  build_dispatch_patches

  export NAMESPACE="$namespace"
  export ORG_ID="${namespace//-/.}"
  export APP_INSTANCE_LABEL="fint-flyt-egrunnerverv-gateway_${namespace//-/_}"
  export KAFKA_TOPIC="${namespace}.flyt.*"
  export INGRESS_BASE_PATH="/api/egrunnerverv/instances/${ORG_NUMBER}"
  export ENV_PATCHES
  export DISPATCH_PATCHES

  if [[ "$environment" == "beta" ]]; then
    export URL_BASE_PATH="/beta"
    export INGRESS_BASE_PATH="/beta/api/egrunnerverv/instances/${ORG_NUMBER}"
    export STARTUP_PATH="/beta/actuator/health"
    export READINESS_PATH="/beta/actuator/health/readiness"
    export LIVENESS_PATH="/beta/actuator/health/liveness"
    export METRICS_PATH="/beta/actuator/prometheus"
    export DISPATCH_ONEPASSWORD_PATH="vaults/aks-beta-vault/items/novari-flyt-egrunnerverv-gateway-out-v2"
    export SLACK_ONEPASSWORD_PATH="vaults/aks-beta-vault/items/fint-flyt-v1-slack-webhook"
  else
    export URL_BASE_PATH=""
    export STARTUP_PATH="/actuator/health"
    export READINESS_PATH="/actuator/health/readiness"
    export LIVENESS_PATH="/actuator/health/liveness"
    export METRICS_PATH="/actuator/prometheus"
    export DISPATCH_ONEPASSWORD_PATH=""
    export SLACK_ONEPASSWORD_PATH=""
  fi

  template="$(choose_template "$environment")"
  target_dir="$ROOT/kustomize/overlays/$dir"

  tmp="$(mktemp "$target_dir/.kustomization.yaml.XXXXXX")"
  envsubst '$NAMESPACE $APP_INSTANCE_LABEL $ORG_ID $KAFKA_TOPIC $INGRESS_BASE_PATH $ENV_PATCHES $DISPATCH_PATCHES $URL_BASE_PATH $STARTUP_PATH $READINESS_PATH $LIVENESS_PATH $METRICS_PATH $DISPATCH_ONEPASSWORD_PATH $SLACK_ONEPASSWORD_PATH' \
    < "$template" > "$tmp"
  mv "$tmp" "$target_dir/kustomization.yaml"
done < <(find "$ROOT/kustomize/overlays" -name kustomization.yaml -print | sort)
