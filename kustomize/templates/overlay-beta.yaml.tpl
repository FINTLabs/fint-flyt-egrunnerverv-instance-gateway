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
      - op: add
        path: "/spec/url/basePath"
        value: ""
      - op: replace
        path: "/spec/ingress/basePath"
        value: "$INGRESS_BASE_PATH"
      - op: add
        path: "/spec/env/-"
        value:
          name: novari.kafka.topic.org-id
          value: "$NAMESPACE"
      - op: add
        path: "/spec/env/-"
        value:
          name: "novari.telemetry.org-id"
          value: "$ORG_ID"$ENV_PATCHES$DISPATCH_PATCHES
      - op: add
        path: "/spec/env/-"
        value:
          name: server.servlet.context-path
          value: "$URL_BASE_PATH"
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
      - op: add
        path: "/spec/env/-"
        value:
          name: "OTEL_EXPORTER_OTLP_ENDPOINT"
          value: "http://alloy.flais-system.svc.cluster.local:4318"
    target:
      kind: Application
      name: fint-flyt-egrunnerverv-gateway

  - patch: |-
      - op: replace
        path: "/spec/itemPath"
        value: "$DISPATCH_ONEPASSWORD_PATH"
    target:
      kind: OnePasswordItem
      name: novari-flyt-egrunnerverv-dispatch-oauth2-client

  - patch: |-
      - op: replace
        path: "/spec/itemPath"
        value: "$SLACK_ONEPASSWORD_PATH"
    target:
      kind: OnePasswordItem
      name: fint-flyt-v1-slack-webhook
