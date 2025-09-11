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
INSERT INTO vocabulary.user (id, username, email, keycloakId, external_id)
VALUES (1000000, 'enrico', 'enrico@gmail.com', 'f95cb50f-5f3b-4b71-9f8b-3495d47622cf',  '00000000-0000-0000-0000-000000000007'),
       (1000001, 'lucio', 'lucio@gmail.com', '0a3d2c4b-8e79-4a5c-95d1-13c96c2ef4b7', '00000000-0000-0000-0000-000000000008' )
;

--test data for word
INSERT INTO vocabulary.word (id, sentence, translation, description, language, language_to,
                             user_id, external_id)
                    VALUES (1000000, 'Hello', 'Salve', 'a gentle salutation', 'English', 'Italian',
                            1000000, '00000000-0000-0000-0000-000000000001'),
                           (1000001, 'my house', 'La mia casa', 'my own house', 'English', 'Italian',
                            1000000, '00000000-0000-0000-0000-000000000002'),
                           (1000002, 'cat', 'gatto', null, 'English', 'Italian',
                            1000000, '00000000-0000-0000-0000-000000000003'),
                           (1000003, 'tomcat', 'gattone', 'my big gat', 'English', 'Italian',
                            1000000, '00000000-0000-0000-0000-000000000004'),
                           (1000004, 'Latte', 'die Milk', 'milk', 'English', 'German',
                            1000000, '00000000-0000-0000-0000-000000000005'),
                           (1000005, 'Hello', 'Ciao', 'a gentle salutation', 'English', 'Italian',
                            1000001, '00000000-0000-0000-0000-000000000006')
;

