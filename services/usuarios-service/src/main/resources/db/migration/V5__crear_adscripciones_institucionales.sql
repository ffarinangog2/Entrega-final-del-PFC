CREATE TABLE adscripciones_institucionales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_id UUID NOT NULL,
    tipo_ambito VARCHAR(20) NOT NULL,
    ambito_id UUID NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_adscripciones_perfil
        FOREIGN KEY (perfil_id) REFERENCES perfiles(id),
    CONSTRAINT ck_adscripciones_tipo_ambito
        CHECK (tipo_ambito IN ('CARRERA', 'FACULTAD')),
    CONSTRAINT uk_adscripciones_perfil_tipo_ambito
        UNIQUE (perfil_id, tipo_ambito, ambito_id)
);

CREATE INDEX idx_adscripciones_perfil
    ON adscripciones_institucionales (perfil_id);

CREATE INDEX idx_adscripciones_ambito
    ON adscripciones_institucionales (tipo_ambito, ambito_id);
