# Documentación académica

## Fuente oficial

La única fuente oficial y acumulativa del informe final es [`main.tex`](main.tex).
GitHub Actions la compila desde `docs/` y publica `main.pdf` como artifact
`informe-final-scli`. El PDF oficial no se versiona necesariamente en Git.

Para reproducir el mismo procedimiento desde la raíz:

```bash
cd docs
pdflatex -interaction=nonstopmode -halt-on-error main.tex
bibtex main
pdflatex -interaction=nonstopmode -halt-on-error main.tex
pdflatex -interaction=nonstopmode -halt-on-error main.tex
```

Las fuentes modulares vigentes que consume el informe están en
`entrega-4/secciones/`. Los ADR, diagramas, evidencias técnicas, contratos
OpenAPI e informes ISO son documentación complementaria trazable.

## Documentación histórica

`entrega-3/` y `entrega-4/` conservan fuentes y PDFs de entregas anteriores. Son
snapshots históricos y no reemplazan `docs/main.tex`. Sus inconsistencias
editoriales se preservan cuando forman parte del material originalmente entregado.

Los PDFs históricos pueden no contener integraciones documentales posteriores.
Para consultar el estado acumulativo se debe usar la fuente oficial o el artifact
producido por el workflow de documentación.

## Evidencias visuales pendientes

No hay capturas versionadas del dashboard real de Grafana ni del flujo QR en un
dispositivo. Si se incorporan después, deben proceder de un entorno real, omitir
secretos y datos personales, e identificar SHA, fecha, zona horaria y entorno.
