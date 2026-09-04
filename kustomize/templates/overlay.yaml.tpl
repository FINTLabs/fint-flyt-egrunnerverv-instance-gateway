apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
namespace: $NAMESPACE

resources:
  - ../../../base

labels:
  - pairs:
      app.kubernetes.io/instance: $APP_INSTANCE_LABEL
      fintlabs.no/org-id: $ORG_ID

patches:
  - patch: |-
      - op: replace
        path: "/spec/kafka/acls/0/topic"
        value: "$KAFKA_TOPIC"
      - op: replace
        path: "/spec/orgId"
        value: "$ORG_ID"
      - op: replace
        path: "/spec/ingress/basePath"
        value: "$INGRESS_BASE_PATH"
      - op: add
        path: "/spec/env/-"
        value:
          name: novari.kafka.topic.org-id
          value: "$NAMESPACE"$ENV_PATCHES$DISPATCH_PATCHES
      # ServiceNow returnerer 401 hvis client_secret URL-encodes i
      # Authorization: Basic-headeren (slik RFC 6749 §2.3.1 krever, og
      # som Spring Security gjør by default). Verifisert mot
      # vigoikstest.service-now.com 2026-05-29: client_secret med
      # spesialtegn feiler i Basic-flow, men client_secret_post med
      # credentials i body fungerer. Overstyrer derfor metoden her i
      # stedet for å regenerere secret-et eller patche Spring.
      - op: add
        path: "/spec/env/-"
        value:
          name: SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_EGRUNNERVERV_CLIENT_AUTHENTICATION_METHOD
          value: "client_secret_post"
      - op: replace
        path: "/spec/probes/startup/path"
        value: "$STARTUP_PATH"
      - op: replace
        path: "/spec/probes/readiness/path"
        value: "$READINESS_PATH"
      - op: replace
        path: "/spec/probes/liveness/path"
        value: "$LIVENESS_PATH"
      - op: replace
        path: "/spec/observability/metrics/path"
        value: "$METRICS_PATH"
    target:
      kind: Application
      name: fint-flyt-egrunnerverv-gateway
