-- Hibernate mapea java.lang.Integer a INTEGER/int4. La ampliación desde
-- SMALLINT conserva todos los valores existentes y la restricción de nivel.
ALTER TABLE planificaciones_semestre
    ALTER COLUMN nivel TYPE INTEGER;
