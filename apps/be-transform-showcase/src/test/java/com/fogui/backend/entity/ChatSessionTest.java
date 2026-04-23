package com.fogui.backend.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatSession entity.
 * Tests entity defaults and behavior.
 */
@DisplayName("ChatSession Entity")
class ChatSessionTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .build();
    }

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("should have empty messages array by default")
        void shouldHaveEmptyMessagesArrayByDefault() {
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .build();

            assertEquals("[]", session.getMessages());
        }

        @Test
        @DisplayName("should have empty context object by default")
        void shouldHaveEmptyContextObjectByDefault() {
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .build();

            assertEquals("{}", session.getContext());
        }

        @Test
        @DisplayName("should have createdAt set")
        void shouldHaveCreatedAtSet() {
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .build();

            assertNotNull(session.getCreatedAt());
        }

        @Test
        @DisplayName("should have updatedAt set")
        void shouldHaveUpdatedAtSet() {
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .build();

            assertNotNull(session.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Messages and Context")
    class MessagesAndContext {

        @Test
        @DisplayName("should allow setting custom messages JSON")
        void shouldAllowSettingCustomMessagesJson() {
            String messagesJson = "[{\"role\":\"user\",\"content\":\"Hello\"}]";

            ChatSession session = ChatSession.builder()
                    .user(user)
                    .messages(messagesJson)
                    .build();

            assertEquals(messagesJson, session.getMessages());
        }

        @Test
        @DisplayName("should allow setting custom context JSON")
        void shouldAllowSettingCustomContextJson() {
            String contextJson = "{\"intent\":\"greeting\",\"language\":\"en\"}";

            ChatSession session = ChatSession.builder()
                    .user(user)
                    .context(contextJson)
                    .build();

            assertEquals(contextJson, session.getContext());
        }

        @Test
        @DisplayName("should allow updating messages")
        void shouldAllowUpdatingMessages() {
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .build();

            String newMessages = "[{\"role\":\"assistant\",\"content\":\"Hi there!\"}]";
            session.setMessages(newMessages);

            assertEquals(newMessages, session.getMessages());
        }
    }

    @Nested
    @DisplayName("User Association")
    class UserAssociation {

        @Test
        @DisplayName("should associate with user")
        void shouldAssociateWithUser() {
            ChatSession session = ChatSession.builder()
                    .user(user)
                    .build();

            assertEquals(user, session.getUser());
        }
    }
}
