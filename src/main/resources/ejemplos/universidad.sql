CREATE TABLE programa (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL);
CREATE TABLE estudiante (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL,
                         semestre INT, id_programa INT);
CREATE TABLE docente (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL,
                      correo VARCHAR(80), activo BOOLEAN);
CREATE TABLE asignatura (id INT PRIMARY KEY, nombre VARCHAR(60) NOT NULL,
                         creditos INT NOT NULL, id_programa INT, id_docente INT);
CREATE TABLE matricula (id INT PRIMARY KEY, id_estudiante INT NOT NULL,
                        id_asignatura INT NOT NULL, fecha DATE NOT NULL, nota DECIMAL);
