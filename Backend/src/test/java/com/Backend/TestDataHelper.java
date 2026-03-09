package com.Backend;


import com.Backend.Chat.Enums.ChatType;
import com.Backend.Chat.dto.Chat.ChatDTO;
import com.Backend.Chat.entity.Chat.Chat;
import com.Backend.Chat.entity.Message.Message;
import com.Backend.User.entity.MusicGenre;
import com.Backend.User.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Esta clase tiene la finalidad de ser un apoyo para los test de la aplicación.
 * Dado que cada tests que se realiza a un servicio debe tener su setup correspondiente,
 * es más que garantizado que se va a replicar mucho codigo en cada uno de ellos y se van
 * a realizar muchas querys a la base de datos para hacer lo mismo, por lo tanto se implementa
 * esta clase para mejorar el rendimiento de la aplicacion
 */
public final class TestDataHelper {

    private TestDataHelper(){}

    private static long nextUserId = 1;

    public static User createTestUser(Long id, String name, String username, String email, String password, String phoneNumber, String location, String biography, String profilePhotoUrl, List<String> socialNetworks, Set<MusicGenre> musicGenres, LocalDateTime createdAt, LocalDateTime updatedAt) {
        long idVal = id != null ? id : nextUserId++;
        String nameVal = name != null ? name : "Name" + idVal;
        String userVal = username != null ? username : "user" + idVal;
        String emailVal = email != null ? email : userVal + "@test.com";
        String passVal = password != null ? password : "pass" + idVal;
        String phoneVal = phoneNumber != null ? phoneNumber : "000-000-" + String.format("%04d", idVal);
        String locVal = location != null ? location : "Location" + idVal;
        String bioVal = biography != null ? biography : "";
        String photoVal = profilePhotoUrl != null ? profilePhotoUrl : "default.jpg";
        List<String> socialsVal = socialNetworks != null ? socialNetworks : new ArrayList<>();
        Set<MusicGenre> genresVal = (musicGenres != null) ? musicGenres : new HashSet<>();
        LocalDateTime createdVal = createdAt != null ? createdAt : LocalDateTime.now();
        LocalDateTime updatedVal = updatedAt != null ? updatedAt : LocalDateTime.now();

        return User.builder()
                .id(idVal)
                .name(nameVal)
                .username(userVal)
                .email(emailVal)
                .password(passVal)
                .phoneNumber(phoneVal)
                .location(locVal)
                .biography(bioVal)
                .profilePhotoUrl(photoVal)
                .socialNetworks(socialsVal)
                .musicGenres(genresVal)
                .createdAt(createdVal)
                .updatedAt(updatedVal)
                .build();
    }

    public static List<Message> createMessages(int messageCuantity, Chat chat, User remitter, User receiver) {
        List<Message> messages = new ArrayList<>();
        for (int i = 1; i <= messageCuantity; i++) {
            Message message = new Message();
            message.setId((long) i);
            message.setChat(chat);
            message.setSender(i % 2 == 0 ? remitter : receiver);
            message.setMessageContent("Mensaje " + i);
            messages.add(message);
        }
        return messages;
    }

    public static boolean validateChatPrivacy(ChatDTO chat){
        return chat.getChatType() == ChatType.PRIVATE;
    }
}
