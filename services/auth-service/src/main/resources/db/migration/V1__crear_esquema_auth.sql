CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE permisos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp()
);

CREATE TABLE usuarios_auth (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    perfil_id UUID NOT NULL,

    username VARCHAR(80) NOT NULL UNIQUE,
    email VARCHAR(160) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    activo BOOLEAN NOT NULL DEFAULT TRUE,
    cuenta_bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    intentos_fallidos INT NOT NULL DEFAULT 0,

    bloqueado_hasta TIMESTAMPTZ,
    ultimo_login TIMESTAMPTZ,
    password_actualizado_en TIMESTAMPTZ,

    creado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),

    CONSTRAINT ck_usuarios_auth_intentos
        CHECK (intentos_fallidos >= 0)
);

CREATE TABLE roles_permisos (
    rol_id UUID NOT NULL,
    permiso_id UUID NOT NULL,
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),

    CONSTRAINT pk_roles_permisos
        PRIMARY KEY (rol_id, permiso_id),

    CONSTRAINT fk_roles_permisos_rol
        FOREIGN KEY (rol_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_roles_permisos_permiso
        FOREIGN KEY (permiso_id)
        REFERENCES permisos(id)
        ON DELETE CASCADE
);

CREATE TABLE usuarios_roles (
    usuario_id UUID NOT NULL,
    rol_id UUID NOT NULL,
    asignado_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),

    CONSTRAINT pk_usuarios_roles
        PRIMARY KEY (usuario_id, rol_id),

    CONSTRAINT fk_usuarios_roles_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios_auth(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_usuarios_roles_rol
        FOREIGN KEY (rol_id)
        REFERENCES roles(id)
        ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL,

    token_hash VARCHAR(255) NOT NULL UNIQUE,
    familia_token UUID NOT NULL,

    emitido_en TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),
    expira_en TIMESTAMPTZ NOT NULL,

    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    revocado_en TIMESTAMPTZ,
    reemplazado_por UUID,

    ip_address VARCHAR(64),
    user_agent VARCHAR(500),

    CONSTRAINT fk_refresh_tokens_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios_auth(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_refresh_tokens_reemplazo
        FOREIGN KEY (reemplazado_por)
        REFERENCES refresh_tokens(id),

    CONSTRAINT ck_refresh_tokens_fechas
        CHECK (expira_en > emitido_en)
);

CREATE TABLE intentos_login (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID,

    username_ingresado VARCHAR(160),
    exitoso BOOLEAN NOT NULL,
    motivo VARCHAR(150),

    ip_address VARCHAR(64),
    user_agent VARCHAR(500),

    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT current_timestamp(),

    CONSTRAINT fk_intentos_login_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES usuarios_auth(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_usuarios_auth_perfil
    ON usuarios_auth (perfil_id);

CREATE INDEX idx_refresh_tokens_usuario
    ON refresh_tokens (usuario_id);

CREATE INDEX idx_refresh_tokens_familia
    ON refresh_tokens (familia_token);

CREATE INDEX idx_refresh_tokens_expiracion
    ON refresh_tokens (expira_en);

CREATE INDEX idx_intentos_login_usuario
    ON intentos_login (usuario_id);

CREATE INDEX idx_intentos_login_fecha
    ON intentos_login (fecha_hora);