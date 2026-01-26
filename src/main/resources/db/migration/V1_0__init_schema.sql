-- #%L
-- Vocabulary API
-- %%
-- Copyright (C) 2024 Vocabulary Team
-- %%
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
-- 
--      http://www.apache.org/licenses/LICENSE-2.0
-- 
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
-- #L%
CREATE TABLE vocabulary.user
(
    id          SERIAL,
    keycloakId  character varying(256) NOT NULL UNIQUE,
    username    character varying(256), -- only for audit purpose
    email       character varying(256), -- only for audit purpose,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    external_id UUID                   NOT NULL UNIQUE DEFAULT gen_random_uuid()

);

CREATE TABLE vocabulary.language
(
    id          SERIAL,
    code        VARCHAR(5) UNIQUE NOT NULL, -- ISO code: en, es, de...
    name        character varying(256),     -- only for audit purpose NOT NULL,
    active      BOOLEAN           NOT NULL        DEFAULT TRUE,
    external_id UUID              NOT NULL UNIQUE DEFAULT gen_random_uuid()
);
-- define the user language pair preference
CREATE TABLE vocabulary.user_languages
(
    id          SERIAL,
    user_id         integer                NOT NULL UNIQUE,
    language_id     integer                NOT NULL,
    language_to_id  integer                NOT NULL,
    external_id UUID              NOT NULL UNIQUE DEFAULT gen_random_uuid()
);

CREATE TABLE vocabulary.word
(
    id              SERIAL,
    sentence        character varying(256) NOT NULL,
    translation     TEXT                   NOT NULL,
    description     TEXT,
    language_id     integer                NOT NULL,
    language_to_id  integer                NOT NULL,
    user_id         integer                NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    external_id     UUID                   NOT NULL UNIQUE DEFAULT gen_random_uuid()
);

CREATE TABLE vocabulary.translation_usage
(
    id              SERIAL,
    month DATE NOT NULL UNIQUE,
    cnt BIGINT NOT NULL DEFAULT 0,
    external_id     UUID                   NOT NULL UNIQUE DEFAULT gen_random_uuid()
);









