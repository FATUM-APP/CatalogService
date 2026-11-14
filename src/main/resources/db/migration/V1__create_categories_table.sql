CREATE TABLE categories (
    name              VARCHAR(30)   NOT NULL PRIMARY KEY,
    slug              VARCHAR(30)   NOT NULL UNIQUE,
    scheduling_model  VARCHAR(14)   NOT NULL,
    is_able           BOOLEAN       NOT NULL DEFAULT TRUE,
    logo_key          VARCHAR(512),
    parent_name       VARCHAR(30),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_name) REFERENCES categories (name)
);
