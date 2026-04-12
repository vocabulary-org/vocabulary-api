package org.enricogiurin.vocabulary.api.notification;

/*-
 * #%L
 * Vocabulary API
 * %%
 * Copyright (C) 2024 - 2026 Vocabulary Team
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


import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.enricogiurin.vocabulary.api.VocabularyTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Import(VocabularyTestConfiguration.class)
class EmailServiceTest {

  @RegisterExtension
  static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
      .withConfiguration(GreenMailConfiguration.aConfig().withUser("test@localhost", "password"))
      .withPerMethodLifecycle(false);

  @DynamicPropertySource
  static void configureMailProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.mail.host", () -> "localhost");
    registry.add("spring.mail.port", () -> ServerSetupTest.SMTP.getPort());
    registry.add("spring.mail.username", () -> "test@localhost");
    registry.add("spring.mail.password", () -> "password");
    registry.add("application.notification.admin-email", () -> "admin@test.com");
    registry.add("application.notification.from-email", () -> "noreply@test.com");
  }

  @Autowired
  EmailService emailService;

  @BeforeEach
  void purgeMailbox() throws FolderException {
    greenMail.purgeEmailFromAllMailboxes();
  }

  @Test
  void send() throws MessagingException {
    emailService.send("recipient@test.com", "Hello World", "This is the body.");

    MimeMessage[] messages = greenMail.getReceivedMessages();
    assertThat(messages).hasSize(1);
    assertThat(messages[0].getSubject()).isEqualTo("Hello World");
    assertThat(GreenMailUtil.getBody(messages[0]).trim()).isEqualTo("This is the body.");
    assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("recipient@test.com");
  }

  @Test
  void notifyAdmin() throws MessagingException {
    emailService.notifyAdmin("New user registered", "User john@example.com just logged in.");

    MimeMessage[] messages = greenMail.getReceivedMessages();
    assertThat(messages).hasSize(1);
    assertThat(messages[0].getAllRecipients()[0].toString()).isEqualTo("admin@test.com");
    assertThat(messages[0].getFrom()[0].toString()).isEqualTo("noreply@test.com");
    assertThat(messages[0].getSubject()).isEqualTo("New user registered");
  }
}
