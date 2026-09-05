-- =====================================================================
-- ACADEMICO-LABORATORIOS-SERVICE
-- V1: Creación de esquema (dominio académico + infraestructura de labs)
-- =====================================================================

-- ---------------------------------------------------------------------
-- DOMINIO ACADÉMICO
-- ---------------------------------------------------------------------

CREATE TABLE facultades (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(150) NOT NULL,

    descripcion TEXT,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE carreras (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    facultad_id UUID NOT NULL,

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(150) NOT NULL,

    descripcion TEXT,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_carreras_facultad
        FOREIGN KEY (facultad_id)
            REFERENCES facultades(id)
);


CREATE TABLE materias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    carrera_id UUID NOT NULL,

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(150) NOT NULL,

    numero_horas INTEGER NOT NULL DEFAULT 0,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_materias_carrera
        FOREIGN KEY (carrera_id)
            REFERENCES carreras(id),

    CONSTRAINT ck_materias_numero_horas
        CHECK (numero_horas >= 0)
);


CREATE TABLE periodos_lectivos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(100) NOT NULL,

    fecha_inicio DATE NOT NULL,

    fecha_fin DATE NOT NULL,

    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADO',

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ck_periodos_fechas
        CHECK (fecha_fin > fecha_inicio),

    CONSTRAINT ck_periodos_estado
        CHECK (estado IN ('PLANIFICADO', 'ACTIVO', 'FINALIZADO'))
);


-- ---------------------------------------------------------------------
-- DOMINIO INFRAESTRUCTURA
-- ---------------------------------------------------------------------

CREATE TABLE campus (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(150) NOT NULL,

    direccion VARCHAR(255),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE bloques (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    campus_id UUID NOT NULL,

    codigo VARCHAR(20) NOT NULL,

    nombre VARCHAR(100) NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bloques_campus
        FOREIGN KEY (campus_id)
            REFERENCES campus(id),

    CONSTRAINT uq_bloques_campus_codigo
        UNIQUE (campus_id, codigo)
);


CREATE TABLE pisos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    bloque_id UUID NOT NULL,

    numero INTEGER NOT NULL,

    descripcion VARCHAR(200),

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pisos_bloque
        FOREIGN KEY (bloque_id)
            REFERENCES bloques(id),

    CONSTRAINT uq_pisos_bloque_numero
        UNIQUE (bloque_id, numero)
);


CREATE TABLE tipos_equipo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(100) NOT NULL,

    descripcion TEXT,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE laboratorios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    piso_id UUID NOT NULL,

    codigo VARCHAR(20) NOT NULL UNIQUE,

    nombre VARCHAR(150) NOT NULL,

    capacidad INTEGER NOT NULL,

    descripcion TEXT,

    estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE',

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_laboratorios_piso
        FOREIGN KEY (piso_id)
            REFERENCES pisos(id),

    CONSTRAINT ck_laboratorios_capacidad
        CHECK (capacidad > 0),

    CONSTRAINT ck_laboratorios_estado
        CHECK (estado IN ('DISPONIBLE', 'OCUPADO', 'MANTENIMIENTO', 'INACTIVO'))
);


CREATE TABLE equipos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    laboratorio_id UUID NOT NULL,

    tipo_equipo_id UUID NOT NULL,

    codigo_inventario VARCHAR(30) NOT NULL UNIQUE,

    numero_serie VARCHAR(60) UNIQUE,

    marca VARCHAR(60),

    modelo VARCHAR(60),

    procesador VARCHAR(100),

    memoria_ram VARCHAR(30),

    almacenamiento VARCHAR(30),

    direccion_ip VARCHAR(45),

    direccion_mac VARCHAR(17),

    estado VARCHAR(20) NOT NULL DEFAULT 'OPERATIVO',

    observacion TEXT,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_equipos_laboratorio
        FOREIGN KEY (laboratorio_id)
            REFERENCES laboratorios(id),

    CONSTRAINT fk_equipos_tipo_equipo
        FOREIGN KEY (tipo_equipo_id)
            REFERENCES tipos_equipo(id),

    CONSTRAINT ck_equipos_estado
        CHECK (estado IN ('OPERATIVO', 'CON_FALLAS', 'MANTENIMIENTO', 'FUERA_DE_SERVICIO'))
);


-- ---------------------------------------------------------------------
-- HORARIOS ACADÉMICOS (conecta académico + infraestructura + docente externo)
-- ---------------------------------------------------------------------

CREATE TABLE horarios_academicos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    materia_id UUID NOT NULL,

    periodo_lectivo_id UUID NOT NULL,

    laboratorio_id UUID,

    docente_id UUID NOT NULL,

    dia_semana VARCHAR(15) NOT NULL,

    hora_inicio TIME NOT NULL,

    hora_fin TIME NOT NULL,

    paralelo VARCHAR(10) NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_horarios_materia
        FOREIGN KEY (materia_id)
            REFERENCES materias(id),

    CONSTRAINT fk_horarios_periodo
        FOREIGN KEY (periodo_lectivo_id)
            REFERENCES periodos_lectivos(id),

    CONSTRAINT fk_horarios_laboratorio
        FOREIGN KEY (laboratorio_id)
            REFERENCES laboratorios(id),

    -- docente_id es un UUID externo (usuarios-service): SIN FK entre bases de datos

    CONSTRAINT ck_horarios_dia_semana
        CHECK (dia_semana IN ('LUNES','MARTES','MIERCOLES','JUEVES','VIERNES','SABADO','DOMINGO')),

    CONSTRAINT ck_horarios_horas
        CHECK (hora_fin > hora_inicio)
);


-- ---------------------------------------------------------------------
-- ÍNDICES (para filtros y joins frecuentes)
-- ---------------------------------------------------------------------

CREATE INDEX idx_carreras_facultad ON carreras(facultad_id);
CREATE INDEX idx_carreras_activo ON carreras(activo);

CREATE INDEX idx_materias_carrera ON materias(carrera_id);
CREATE INDEX idx_materias_activo ON materias(activo);

CREATE INDEX idx_bloques_campus ON bloques(campus_id);

CREATE INDEX idx_pisos_bloque ON pisos(bloque_id);

CREATE INDEX idx_laboratorios_piso ON laboratorios(piso_id);
CREATE INDEX idx_laboratorios_estado ON laboratorios(estado);
CREATE INDEX idx_laboratorios_activo ON laboratorios(activo);

CREATE INDEX idx_equipos_laboratorio ON equipos(laboratorio_id);
CREATE INDEX idx_equipos_tipo_equipo ON equipos(tipo_equipo_id);
CREATE INDEX idx_equipos_estado ON equipos(estado);

CREATE INDEX idx_horarios_materia ON horarios_academicos(materia_id);
CREATE INDEX idx_horarios_periodo ON horarios_academicos(periodo_lectivo_id);
CREATE INDEX idx_horarios_laboratorio ON horarios_academicos(laboratorio_id);
CREATE INDEX idx_horarios_docente ON horarios_academicos(docente_id);
