SELECT nombre FROM estudiante WHERE semestre >= 4;

SELECT estudiante.nombre, programa.nombre
FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id
WHERE estudiante.semestre >= 4 ORDER BY estudiante.nombre DESC;

INSERT INTO estudiante (id, nombre) VALUES (1, 'Ana');

SELECT nombr FROM estudiante WHERE semestre = 'cuarto';

SELECT id FROM estudiante INNER JOIN programa ON estudiante.id_programa = programa.id;

INSERT INTO estudiante (id, nombre) VALUES (1);

INSERT INTO matricula VALUES (1, 1, 1, '2026-02-30', 4.5);

UPDATE asignatura SET creditos = 4 WHERE id_docente IS NOT NULL AND (id_programa = 1 OR id_programa = 2);

SELECT nombre estudiante;

DELETE FROM estudiante;

SELECT * FROM estudiante WHERE semestre = NULL;
