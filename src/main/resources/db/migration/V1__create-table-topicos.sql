CREATE TABLE topicos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    titulo VARCHAR(100) NOT NULL,
    mensagem VARCHAR(255) NOT NULL,
    data_criacao DATETIME NOT NULL,
    status VARCHAR(100) NOT NULL,
    autor VARCHAR(100) NOT NULL,
    curso VARCHAR(100) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    PRIMARY KEY(id),
    UNIQUE (titulo, mensagem)
);