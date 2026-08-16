CREATE TABLE perfiles (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          identificacion VARCHAR(20) UNIQUE,

                          nombres VARCHAR(100) NOT NULL,

                          apellidos VARCHAR(100) NOT NULL,

                          email_institucional VARCHAR(150) NOT NULL UNIQUE,

                          email_personal VARCHAR(150),

                          telefono VARCHAR(20),

                          direccion VARCHAR(255),

                          fecha_nacimiento DATE,

                          foto_url TEXT,

                          activo BOOLEAN NOT NULL DEFAULT TRUE,

                          creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE docentes (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          perfil_id UUID NOT NULL UNIQUE,

                          codigo_docente VARCHAR(30) UNIQUE,

                          titulo_academico VARCHAR(100),

                          departamento VARCHAR(100),

                          tipo_contrato VARCHAR(30),

                          dedicacion VARCHAR(30),

                          activo BOOLEAN NOT NULL DEFAULT TRUE,

                          creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_docentes_perfil
                              FOREIGN KEY (perfil_id)
                                  REFERENCES perfiles(id)
);


CREATE TABLE estudiantes (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                             perfil_id UUID NOT NULL UNIQUE,

                             matricula VARCHAR(30) NOT NULL UNIQUE,

                             carrera_id UUID,

                             semestre INTEGER,

                             activo BOOLEAN NOT NULL DEFAULT TRUE,

                             creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT ck_estudiantes_semestre
                                 CHECK (
                                     semestre IS NULL
                                         OR semestre BETWEEN 1 AND 20
                                     ),

                             CONSTRAINT fk_estudiantes_perfil
                                 FOREIGN KEY (perfil_id)
                                     REFERENCES perfiles(id)
);


CREATE TABLE tecnicos (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                          perfil_id UUID NOT NULL UNIQUE,

                          codigo_tecnico VARCHAR(30) NOT NULL UNIQUE,

                          especialidad VARCHAR(100),

                          activo BOOLEAN NOT NULL DEFAULT TRUE,

                          creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                          CONSTRAINT fk_tecnicos_perfil
                              FOREIGN KEY (perfil_id)
                                  REFERENCES perfiles(id)
);


CREATE TABLE administradores (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                 perfil_id UUID NOT NULL UNIQUE,

                                 codigo_administrador VARCHAR(30) NOT NULL UNIQUE,

                                 cargo VARCHAR(100),

                                 piso_id UUID,

                                 activo BOOLEAN NOT NULL DEFAULT TRUE,

                                 creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT fk_administradores_perfil
                                     FOREIGN KEY (perfil_id)
                                         REFERENCES perfiles(id)
);


CREATE INDEX idx_perfiles_nombres
    ON perfiles(nombres);


CREATE INDEX idx_perfiles_apellidos
    ON perfiles(apellidos);


CREATE INDEX idx_perfiles_activo
    ON perfiles(activo);


CREATE INDEX idx_estudiantes_carrera
    ON estudiantes(carrera_id);