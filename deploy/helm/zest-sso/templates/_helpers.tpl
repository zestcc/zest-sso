{{- define "zest-sso.name" -}}
zest-sso
{{- end }}

{{- define "zest-sso.fullname" -}}
{{ include "zest-sso.name" . }}
{{- end }}
