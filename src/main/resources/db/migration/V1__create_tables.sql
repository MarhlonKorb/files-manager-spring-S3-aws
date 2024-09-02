CREATE TABLE IF NOT EXISTS usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(200),
    email VARCHAR(100),
    role VARCHAR(2),
    ativo BOOLEAN,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP
);


CREATE TABLE IF NOT EXISTS file (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(200),
    url VARCHAR(200),
    usuario_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP,
    FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
