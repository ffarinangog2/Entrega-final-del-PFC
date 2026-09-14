-- Conserva referencias históricas, pero impide asignar roles fuera del modelo funcional vigente.
UPDATE roles
SET activo = FALSE, actualizado_en = CURRENT_TIMESTAMP
WHERE codigo IN ('TECNICO', 'DECANO') AND activo = TRUE;
