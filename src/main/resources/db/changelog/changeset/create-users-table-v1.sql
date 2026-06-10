--liquibase formatted sql

--changeset jugoouttahere:1

CREATE TABLE users
(
    id       BIGSERIAL PRIMARY KEY,
    login VARCHAR(128) UNIQUE NOT NULL,
    password VARCHAR(256)        NOT NULL
);