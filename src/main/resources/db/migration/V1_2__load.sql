---
-- #%L
-- Vocabulary API
-- %%
-- Copyright (C) 2024 - 2025 Vocabulary Team
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
INSERT INTO language(code, name)
VALUES ('en', 'English'),
       ('es', 'Spanish'),
       ('fr', 'French'),
       ('de', 'German'),
       ('it', 'Italian'),
       ('ru', 'Russian'),
       ('pt', 'Portuguese'),
       ('zh', 'Chinese'),
       ('ja', 'Japanese'),
       ('ar', 'Arabic'),
       ('hi', 'Hindi'),
       ('ko', 'Korean'),
       ('tr', 'Turkish'),
       ('nl', 'Dutch'),
       ('pl', 'Polish'),
       ('sv', 'Swedish');


-- Tags
INSERT INTO vocabulary.tag (name, description) VALUES
('IT',        'Computers, software, programming, internet, and technology'),
('KITCHEN',   'Cooking, recipes, kitchen tools, ingredients, and food preparation'),
('TRAVEL',    'Trips, flights, hotels, destinations, tourism, and exploration'),
('SPORT',     'Sports, athletes, competitions, fitness, and physical activity'),
('HEALTH',    'Medicine, doctors, hospitals, diseases, wellness, and healthcare'),
('WORK',      'Jobs, office life, meetings, colleagues, career, and business'),
('SHOPPING',  'Buying, stores, products, prices, discounts, and commerce'),
('NATURE',    'Animals, plants, landscapes, weather, and the natural world'),
('EDUCATION', 'Schools, studying, teachers, exams, universities, and learning'),
('FAMILY',    'Parents, children, relatives, marriage, and family relationships'),
('FOOD',      'Restaurants, meals, dishes, flavors, cuisine, and dining out'),
('TRANSPORT', 'Cars, trains, buses, planes, traffic, and getting around'),
('SOCIAL',    'Friends, relationships, parties, dating, and social interactions'),
('FINANCE',   'Money, banks, investments, taxes, savings, and financial matters'),
('HOLIDAY',   'Vacations, beaches, leisure, relaxation, and time off');

