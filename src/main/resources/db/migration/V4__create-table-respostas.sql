CREATE TABLE respostas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    mensagem VARCHAR(255) NOT NULL,
    topico_id BIGINT NOT NULL,
    data_criacao DATETIME NOT NULL,
    autor_id BIGINT NOT NULL,
    solucao BOOLEAN DEFAULT FALSE,
    PRIMARY KEY(id),
    FOREIGN KEY (topico_id) REFERENCES topicos(id),
    FOREIGN KEY (autor_id) REFERENCES usuarios(id)
);