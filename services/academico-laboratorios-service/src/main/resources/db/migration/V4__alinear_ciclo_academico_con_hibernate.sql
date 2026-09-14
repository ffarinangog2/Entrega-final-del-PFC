-- Hibernate mapea java.lang.Integer a INTEGER/int4. La ampliacion desde
-- SMALLINT conserva los ciclos existentes y la restriccion de 1 a 2.
ALTER TABLE periodos_lectivos
    ALTER COLUMN ciclo_academico TYPE INTEGER;
