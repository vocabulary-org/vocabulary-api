package org.enricogiurin.vocabulary.api.service;

import com.yourrents.services.common.searchable.Searchable;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.enricogiurin.vocabulary.api.model.Word;
import org.enricogiurin.vocabulary.api.repository.UserRepository;
import org.enricogiurin.vocabulary.api.repository.WordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WordService {

  private final UserRepository userRepository;
  private final WordRepository wordRepository;

  public Word createNewWord(Word word, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.create(word, userId);
  }

  public Word updateAnExistingWord(UUID uuid, Word word, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.update(uuid, word, userId);
  }

  public void deleteAnExistingWord(UUID uuid, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    wordRepository.delete(uuid, userId);
  }

  public Optional<Word> findByExternalId(UUID externalId, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.findByExternalId(externalId, userId);
  }

  public Page<Word> find(Searchable filter, Pageable pageable, String subject) {
    Integer userId = userRepository.findUserIdByKeycloakId(subject);
    return wordRepository.find(filter, pageable, userId);
  }


}
