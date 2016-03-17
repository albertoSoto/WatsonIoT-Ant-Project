package com.cit.usacycling.ant.background.db.repositories;

import com.cit.usacycling.ant.background.db.Message;

import java.util.HashSet;

/**
 * Created by nikolay.nikolov on 15.03.2016
 */
public interface MessageRepositoryInterface {
    boolean insertMessage(String message);

    boolean markMessageAsSent(int messageId);

    Message getMessageById(int messageId);

    HashSet<Message> getMessagesToSend();

    boolean clearAllUnsentMessages();
}
