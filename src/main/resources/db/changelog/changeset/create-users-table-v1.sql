--liquibase formatted sql

--changeset jugoouttahere:1

CREATE TABLE users
(
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100)        NOT NULL
);