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
INSERT INTO vocabulary.word (id, sentence, translation, description, language_id, language_to_id, external_id)
VALUES (1000000, 'Hello', 'Salve', null, 1, 20, '00000000-0000-0000-0000-000000000001'),
       (1000001, 'my house', 'La mia casa', 'my own house', 1, 20, '00000000-0000-0000-0000-000000000002'),
       (1000002, 'cat', 'gatto', null, 1, 20, '00000000-0000-0000-0000-000000000003'),
       (1000003, 'tomcat',  'gattone', 'my big gat', 1, 20, '00000000-0000-0000-0000-000000000004'),
       (1000004, 'Latte',  'die Milk', 'milk', 20, 11, '00000000-0000-0000-0000-000000000005')
;

