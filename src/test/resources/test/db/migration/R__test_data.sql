---
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
---


--test data for word
INSERT INTO vocabulary.word (id, sentence, language_id, external_id)
VALUES (1000000, 'Hello', 1, '00000000-0000-0000-0000-000000000001'),
       (1000001, 'my house', 1, '00000000-0000-0000-0000-000000000002'),
       (1000002, 'cat', 1, '00000000-0000-0000-0000-000000000003'),
       (1000003, 'tomcat', 1, '00000000-0000-0000-0000-000000000004')
;

--test data for translation
INSERT INTO vocabulary.translation (id, translation_content, language_id, word_id, external_id)
VALUES (1000000, 'Salve', 20, 1000000, '00000000-0000-0000-0000-000000000001'),
       (1000001, 'Hallo', 11, 1000000, '00000000-0000-0000-0000-000000000002'),
       (1000002, 'Gatto', 20, 1000002, '00000000-0000-0000-0000-000000000003')
;

