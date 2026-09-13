-- Hibernate mapea java.lang.Integer a INTEGER/int4. La ampliación desde
-- SMALLINT conserva los niveles existentes y la restricción de 1 a 10.
ALTER TABLE materias
    ALTER COLUMN nivel TYPE INTEGER;
