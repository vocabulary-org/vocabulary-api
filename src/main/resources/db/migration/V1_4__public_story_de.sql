-- #%L
-- Vocabulary API
-- %%
-- Copyright (C) 2024 - 2026 Vocabulary Team
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

-- ---------------------------------------------------------------------------
-- German gap-fill story assessment (issue #127)
--   public_story_de            : the story, with {{n}} markers in the body
--   public_story_de_gap        : one missing word per {{n}} marker (linked by position)
--   public_story_de_gap_option : the combo-box choices for a gap (linked by gap_id)
-- No translations, no score persistence: the UI grades using is_correct.
-- ---------------------------------------------------------------------------

CREATE TYPE vocabulary.deutsch_level AS ENUM ('A1', 'A2', 'B1', 'B2', 'C1', 'C2');

CREATE TYPE vocabulary.gap_category AS ENUM (
    'ARTICLE', 'ADJECTIVE_ENDING', 'PRONOUN', 'PREPOSITION', 'VERB_FORM'
);

CREATE TYPE vocabulary.grammatical_case AS ENUM (
    'NOMINATIV', 'AKKUSATIV', 'DATIV', 'GENITIV'
);

CREATE TABLE vocabulary.public_story_de
(
    id          SERIAL PRIMARY KEY,
    title       VARCHAR(256)             NOT NULL,
    level       vocabulary.deutsch_level NOT NULL,
    topic       VARCHAR(64)              NOT NULL,
    body        TEXT                     NOT NULL,
    created_at  TIMESTAMPTZ              NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ              NOT NULL DEFAULT now(),
    external_id UUID                     NOT NULL UNIQUE DEFAULT gen_random_uuid()
);

CREATE TABLE vocabulary.public_story_de_gap
(
    id               SERIAL PRIMARY KEY,
    story_id         INT                         NOT NULL
                       REFERENCES vocabulary.public_story_de (id) ON DELETE CASCADE,
    position         INT                         NOT NULL,
    category         vocabulary.gap_category     NOT NULL,
    grammatical_case vocabulary.grammatical_case NULL,
    created_at       TIMESTAMPTZ                 NOT NULL DEFAULT now(),
    UNIQUE (story_id, position)
);

CREATE TABLE vocabulary.public_story_de_gap_option
(
    id         SERIAL PRIMARY KEY,
    gap_id     INT          NOT NULL
                 REFERENCES vocabulary.public_story_de_gap (id) ON DELETE CASCADE,
    text       VARCHAR(128) NOT NULL,
    is_correct BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order INT          NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (gap_id, sort_order)
);

-- at most one correct option per gap
CREATE UNIQUE INDEX uq_story_de_gap_one_correct
    ON vocabulary.public_story_de_gap_option (gap_id)
    WHERE is_correct;

-- ---------------------------------------------------------------------------
-- Seed: one short A2 story on the topic "food".
-- Body:  "Maria geht in {{1}} Supermarkt. Sie kauft {{2}} Gemüse.
--         Die Verkäuferin hilft {{3}} gern."
-- ---------------------------------------------------------------------------
DO
$$
    DECLARE
        v_story_id INT;
        v_gap_id   INT;
    BEGIN
        INSERT INTO vocabulary.public_story_de (title, level, topic, body)
        VALUES ('Marias Einkauf', 'A2', 'food',
                'Maria geht in {{1}} Supermarkt. Sie kauft {{2}} Gemüse. Die Verkäuferin hilft {{3}} gern.')
        RETURNING id INTO v_story_id;

        -- gap 1: "in ___ Supermarkt" -> den (article, accusative)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 1, 'ARTICLE', 'AKKUSATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'der', FALSE, 1),
               (v_gap_id, 'den', TRUE, 2),
               (v_gap_id, 'dem', FALSE, 3),
               (v_gap_id, 'des', FALSE, 4);

        -- gap 2: "kauft ___ Gemüse" -> frisches (adjective ending, accusative neuter)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 2, 'ADJECTIVE_ENDING', 'AKKUSATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'frische', FALSE, 1),
               (v_gap_id, 'frisches', TRUE, 2),
               (v_gap_id, 'frischen', FALSE, 3),
               (v_gap_id, 'frischem', FALSE, 4);

        -- gap 3: "hilft ___ gern" -> ihr (pronoun, dative; helfen takes dative)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 3, 'PRONOUN', 'DATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'sie', FALSE, 1),
               (v_gap_id, 'ihr', TRUE, 2),
               (v_gap_id, 'ihrer', FALSE, 3),
               (v_gap_id, 'ihnen', FALSE, 4);
    END
$$;

-- ---------------------------------------------------------------------------
-- Seed: a longer B1 story on the topic "music", exercising more categories.
-- Body:  "Gestern Abend war ich auf {{1}} Konzert. Die Band spielte {{2}}
--         Lieder. Mein Freund kennt {{3}} Sänger persönlich. Nach dem Konzert
--         sprachen wir mit {{4}}. Wir {{5}} sehr glücklich nach Hause."
-- ---------------------------------------------------------------------------
DO
$$
    DECLARE
        v_story_id INT;
        v_gap_id   INT;
    BEGIN
        INSERT INTO vocabulary.public_story_de (title, level, topic, body)
        VALUES ('Das Konzert', 'B1', 'music',
                'Gestern Abend war ich auf {{1}} Konzert. Die Band spielte {{2}} Lieder. ' ||
                'Mein Freund kennt {{3}} Sänger persönlich. Nach dem Konzert sprachen wir mit {{4}}. ' ||
                'Wir {{5}} sehr glücklich nach Hause.')
        RETURNING id INTO v_story_id;

        -- gap 1: "auf ___ Konzert" -> einem (article, dative; location after auf)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 1, 'ARTICLE', 'DATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'ein', FALSE, 1),
               (v_gap_id, 'einen', FALSE, 2),
               (v_gap_id, 'einem', TRUE, 3),
               (v_gap_id, 'eines', FALSE, 4);

        -- gap 2: "spielte ___ Lieder" -> schöne (adjective ending, accusative plural, no article)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 2, 'ADJECTIVE_ENDING', 'AKKUSATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'schöne', TRUE, 1),
               (v_gap_id, 'schönen', FALSE, 2),
               (v_gap_id, 'schönes', FALSE, 3),
               (v_gap_id, 'schöner', FALSE, 4);

        -- gap 3: "kennt ___ Sänger" -> diesen (demonstrative pronoun, accusative masculine)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 3, 'PRONOUN', 'AKKUSATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'dieser', FALSE, 1),
               (v_gap_id, 'diesen', TRUE, 2),
               (v_gap_id, 'diesem', FALSE, 3),
               (v_gap_id, 'dieses', FALSE, 4);

        -- gap 4: "sprachen wir mit ___" -> ihm (personal pronoun, dative; mit takes dative)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 4, 'PRONOUN', 'DATIV')
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'er', FALSE, 1),
               (v_gap_id, 'ihn', FALSE, 2),
               (v_gap_id, 'ihm', TRUE, 3),
               (v_gap_id, 'seiner', FALSE, 4);

        -- gap 5: "Wir ___ sehr glücklich nach Hause" -> gingen (verb form, simple past)
        INSERT INTO vocabulary.public_story_de_gap (story_id, position, category, grammatical_case)
        VALUES (v_story_id, 5, 'VERB_FORM', NULL)
        RETURNING id INTO v_gap_id;
        INSERT INTO vocabulary.public_story_de_gap_option (gap_id, text, is_correct, sort_order)
        VALUES (v_gap_id, 'gehen', FALSE, 1),
               (v_gap_id, 'gingen', TRUE, 2),
               (v_gap_id, 'gegangen', FALSE, 3),
               (v_gap_id, 'geht', FALSE, 4);
    END
$$;
