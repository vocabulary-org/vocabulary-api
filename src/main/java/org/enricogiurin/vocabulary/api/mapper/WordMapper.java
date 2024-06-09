package org.enricogiurin.vocabulary.api.mapper;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 Vocabulary Team
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.util.UUID;
import org.enricogiurin.vocabulary.api.model.Word;
import org.jooq.Record;
import org.springframework.stereotype.Component;

@Component
public class WordMapper implements AMapper<Word>{
  public static final String UUID_ALIAS = "uuid";
  public static final String SENTENCE_ALIAS = "sentence";
  public static final String TRANSLATION_ALIAS = "translation";

  @Override
  public Word map(Record record) {

    return new Word(
        record.get(UUID_ALIAS, UUID.class),
        record.get(SENTENCE_ALIAS, String.class),
        record.get(TRANSLATION_ALIAS, String.class)
        );
  }
}
