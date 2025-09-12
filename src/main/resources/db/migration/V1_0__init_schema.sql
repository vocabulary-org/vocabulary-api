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
    username    character varying(256) , -- only for audit purpose
    email       character varying(256) , -- only for audit purpose
    external_id UUID                   NOT NULL UNIQUE DEFAULT gen_random_uuid()
);

CREATE TYPE vocabulary.language AS ENUM (
  'English', 'Spanish', 'French', 'German', 'Italian', 'Russian');
-- on the long term we won't need this
INSERT INTO vocabulary.user (username, email, keycloakId)
VALUES ('enrico', 'enrico@user.com', 'ab8d6366-3e74-47f0-9c9b-114215b1b99f');


CREATE TABLE vocabulary.word
(
    id             SERIAL,
    sentence       character varying(256)       NOT NULL,
    translation    TEXT                         NOT NULL,
    description    TEXT,
    language       vocabulary.language          NOT NULL,
    language_to    vocabulary.language          NOT NULL,
    user_id        integer                      NOT NULL,
    external_id    UUID                         NOT NULL UNIQUE DEFAULT gen_random_uuid()
);








