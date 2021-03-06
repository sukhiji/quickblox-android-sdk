package com.quickblox.snippets.modules;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.exception.BaseServiceException;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.helper.StringifyArrayList;
import com.quickblox.chat.QBGroupChat;
import com.quickblox.chat.QBGroupChatManager;
import com.quickblox.chat.QBPrivacyListsManager;
import com.quickblox.chat.QBPrivateChatManager;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRoster;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBIsTypingListener;
import com.quickblox.chat.listeners.QBMessageListener;
import com.quickblox.chat.listeners.QBPrivacyListListener;
import com.quickblox.chat.listeners.QBPrivateChatManagerListener;
import com.quickblox.chat.listeners.QBRosterListener;
import com.quickblox.chat.listeners.QBSubscriptionListener;
import com.quickblox.chat.model.QBAttachment;
import com.quickblox.chat.model.QBChatHistoryMessage;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialog;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.QBPrivateChat;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.model.QBPrivacyList;
import com.quickblox.chat.model.QBPrivacyListItem;
import com.quickblox.chat.model.QBRosterEntry;
import com.quickblox.core.request.QBRequestCreateBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.quickblox.core.server.BaseService;
import com.quickblox.users.model.QBUser;
import com.quickblox.snippets.ApplicationConfig;
import com.quickblox.snippets.AsyncSnippet;
import com.quickblox.snippets.Snippet;
import com.quickblox.snippets.Snippets;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.RoomInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * User: Igor Khomenko
 * Date: 1.07.14
 */
public class SnippetsChat extends Snippets {

    private static final String TAG = SnippetsChat.class.getSimpleName();

    // Chat service
    //
    private QBChatService chatService;

    // 1-1 Chat
    //
    private QBPrivateChatManager privateChatManager;
    private QBMessageListener<QBPrivateChat> privateChatMessageListener;
    private QBIsTypingListener<QBPrivateChat> privateChatIsTypingListener;
    private QBPrivateChatManagerListener privateChatManagerListener;

    // Group Chat
    //
    private QBGroupChatManager groupChatManager;
    private QBMessageListener<QBGroupChat> groupChatQBMessageListener;
    private QBGroupChat currentChatRoom;

    // Roster
    //
    private QBRoster сhatRoster;
    private QBRosterListener rosterListener;
    private QBSubscriptionListener subscriptionListener;

    // Privacy lists
    //
    private QBPrivacyListsManager privacyListsManager;
    private QBPrivacyListListener privacyListListener;


    private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            NetworkInfo currentNetworkInfo = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            if (currentNetworkInfo.isConnected()) {
                //loginInChat.execute();
            } else {
                Toast.makeText(context, "Not Connected", Toast.LENGTH_LONG).show();
            }
        }
    };

    public SnippetsChat(final Context context) {
        super(context);

        registerReceiver((Activity) context);

        // Init Chat service
        initChatService();

        // Init 1-1 listeners
        initPrivateChatMessageListener();
        initPrivateChatIsTypingListener();

        // Init Group listeners
        initGroupChatMessageListener();

        // Init Roster and its listeners
        initRosterListener();
        initSubscriptionListener();


        snippets.add(loginInChat);
        snippets.add(loginInChatSynchronous);
        //
        snippets.add(isLoggedIn);
        //
        snippets.add(logoutFromChat);
        snippets.add(logoutFromChatSynchronous);
        //
        //
        snippets.add(enableCarbons);
        snippets.add(disableCarbons);
        snippets.add(getCarbonsEnabled);
        //
        //
        snippets.add(sendPrivateMessageWithText);
        snippets.add(sendPrivateMessageExtended);
        //
        //
        snippets.add(sendIsTyping);
        snippets.add(sendStopTyping);
        //
        //
        snippets.add(readMessage);
        //
        //
        snippets.add(joinRoom);
        snippets.add(joinRoomSynchronous);
        //
        snippets.add(sendMessageToRoomWithText);
        snippets.add(sendMessageToRoomExtended);
        //
        snippets.add(getOnlineRoomUsersSynchronous);
        //
        snippets.add(leaveRoom);
        //
        //
        snippets.add(createRoom);
        snippets.add(addUsersToRoom);
        snippets.add(removeUsersFromRoom);
        snippets.add(getRoomUsers);
        snippets.add(getRoomInfo);
        snippets.add(getRooms);
        //
        //
        snippets.add(getDialogs);
        snippets.add(getDialogsSynchronous);
        snippets.add(createDialog);
        snippets.add(createDialogSynchronous);
        snippets.add(updateDialog);
        snippets.add(updateDialogSynchronous);
        snippets.add(deleteDialog);
        snippets.add(deleteDialogSynchronous);
        //
        snippets.add(getMessages);
        snippets.add(getMessagesSynchronous);
        snippets.add(markMessagesAsRead);
        snippets.add(markMessagesAsReadSynchronous);
        snippets.add(deleteMessage);
        snippets.add(deleteMessageSynchronous);
        //
        //
        snippets.add(sendPresence);
        snippets.add(getRosterUsers);
        snippets.add(getUserPresence);
        snippets.add(addUserToRoster);
        snippets.add(removeUserFromRoster);
        snippets.add(confirmAddRequest);
        snippets.add(rejectAddRequest);
        //
        //
        snippets.add(getPrivacyLists);
        snippets.add(getPrivacyList);
        snippets.add(setPrivacyList);
        snippets.add(deletePrivacyList);
        snippets.add(setDefaultPrivacyList);
}

    private void registerReceiver(Activity activity) {
        activity.registerReceiver(wifiReceiver,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }


    private void initChatService(){
        QBChatService.setDebugEnabled(true);

        if (!QBChatService.isInitialized()) {
            QBChatService.init(context);
            chatService = QBChatService.getInstance();
            chatService.addConnectionListener(chatConnectionListener);

        }
    }

    private void initChatPrivateAndGroupManagers(){
        // Get 1-1 chat manager
        //
        privateChatManager = chatService.getPrivateChatManager();

        // Create 1-1 chat manager listener
        //
        privateChatManagerListener = new QBPrivateChatManagerListener() {
            @Override
            public void chatCreated(final QBPrivateChat privateChat, final boolean createdLocally) {
                if(!createdLocally){
                    Log.i(TAG, "adding message listener to new chat");
                    privateChat.addMessageListener(privateChatMessageListener);
                    privateChat.addIsTypingListener(privateChatIsTypingListener);
                }

                log("chatCreated: " + privateChat + ", createdLocally: " + createdLocally);
            }
        };
        //
        privateChatManager.addPrivateChatManagerListener(privateChatManagerListener);


        // Get group chat manager
        //
        groupChatManager = chatService.getGroupChatManager();
    }

    //
    ///////////////////////////////////////////// Login/Logout /////////////////////////////////////////////
    //


    Snippet loginInChat = new Snippet("login to Chat") {
        @Override
        public void execute() {

            // init test user
            QBUser qbUser = new QBUser();
            qbUser.setId(ApplicationConfig.getTestUserID1());
            qbUser.setPassword(ApplicationConfig.getTestUserPasswordForChat1());

            log("login with user: " + qbUser);

            chatService.login(qbUser, new QBEntityCallbackImpl() {
                @Override
                public void onSuccess() {

                    log("success when login");

                    initChatPrivateAndGroupManagers();

//                    // Add Chat message listener
                    initRoster();

                    initPrivacyListsManager();
                    initPrivacyListsListener();
                }

                @Override
                public void onError(List errors) {
                    log("error when login: " + errors);
                }
            });
        }
    };

    Snippet loginInChatSynchronous = new AsyncSnippet("login to Chat (synchronous)", context) {
        @Override
        public void executeAsync() {
            // init test user
            QBUser qbUser = new QBUser();
            qbUser.setId(ApplicationConfig.getTestUserID1());
            qbUser.setPassword(ApplicationConfig.getTestUserPasswordForChat1());

            log("login with user: " + qbUser);

            try {
                chatService.login(qbUser);

            } catch (IOException e) {
                setException(e);
            } catch (SmackException e) {
                setException(e);
            }catch (XMPPException e) {
                setException(e);
            }
        }

        @Override
        protected void postExecute() {
            super.postExecute();
            final Exception exc = getException();

            if (exc == null) {
                initChatPrivateAndGroupManagers();

                log("success when login");

                initRoster();

                initPrivacyListsManager();
                initPrivacyListsListener();
            }else{
                log("error when login: " + exc.getClass().getSimpleName());
            }
        }
    };

    Snippet isLoggedIn = new Snippet("Is logged In") {
        @Override
        public void execute() {
            boolean isLoggedIn = chatService.isLoggedIn();

            log("isLoggedIn:" + isLoggedIn);
        }
    };

    Snippet logoutFromChat = new Snippet("Logout from Chat") {
        @Override
        public void execute() {
            chatService.logout(new QBEntityCallbackImpl() {

                @Override
                public void onSuccess() {
                    log("Logout success");

                    chatService.destroy();
                }

                @Override
                public void onError(final List list) {
                    log("Logout error:" + list);
                }
            });
        }
    };

    Snippet logoutFromChatSynchronous = new AsyncSnippet("Logout from Chat (synchronous)", context) {
        @Override
        public void executeAsync() {
            try {
                chatService.logout();
                //
                chatService.destroy();
            }catch (SmackException.NotConnectedException e){
                setException(e);
            }
        }

        @Override
        protected void postExecute() {
            super.postExecute();
            final Exception exc = getException();

            if (exc == null) {
                log("Logout success");
            }else{
                log("Logout error: " + exc.getClass().getSimpleName());
            }
        }
    };

    ConnectionListener chatConnectionListener = new ConnectionListener() {
        @Override
        public void connected(XMPPConnection connection) {
            log("connected");
        }

        @Override
        public void authenticated(XMPPConnection connection) {
            log("authenticated");
        }

        @Override
        public void connectionClosed() {
            log("connectionClosed");
        }

        @Override
        public void connectionClosedOnError(final Exception e) {
            log("connectionClosedOnError: " + e.getLocalizedMessage());
        }

        @Override
        public void reconnectingIn(final int seconds) {
            if(seconds % 5 == 0) {
                log("reconnectingIn: " + seconds);
            }
        }

        @Override
        public void reconnectionSuccessful() {
            log("reconnectionSuccessful");
        }

        @Override
        public void reconnectionFailed(final Exception error) {
            log("reconnectionFailed: " + error.getLocalizedMessage());
        }
    };


    //
    ////////////////////////////////////////// Carbons /////////////////////////////////////////////
    //

    Snippet enableCarbons = new Snippet("enable carbons") {
        @Override
        public void execute() {
            if(!chatService.isLoggedIn()){
                log("Please login first");
                return;
            }

            try {
                chatService.enableCarbons();
            } catch (XMPPException e) {
                log("enable carbons error: " + e.getLocalizedMessage());
            } catch (SmackException e) {
                log("enable carbons error: " + e.getClass().getSimpleName());
            }
        }
    };

    Snippet disableCarbons = new Snippet("disable carbons") {
        @Override
        public void execute() {
            if(!chatService.isLoggedIn()){
                log("Please login first");
                return;
            }

            try {
                chatService.disableCarbons();
            } catch (XMPPException e) {
                log("disable carbons error: " + e.getLocalizedMessage());
            } catch (SmackException e) {
                log("disable carbons error: " + e.getClass().getSimpleName());
            }

        }
    };

    Snippet getCarbonsEnabled = new Snippet("get carbons enabled") {
        @Override
        public void execute() {
            if(!chatService.isLoggedIn()){
                log("Please login first");
                return;
            }

            boolean isEnabled = chatService.getCarbonsEnabled();
            log("carbons enabled: " + isEnabled);
        }
    };


    //
    ///////////////////////////////////////////// 1-1 Chat /////////////////////////////////////////////
    //


    private void initPrivateChatMessageListener(){
        // Create 1-1 chat is message listener
        //
        privateChatMessageListener = new QBMessageListener<QBPrivateChat>() {
            @Override
            public void processMessage(QBPrivateChat privateChat, final QBChatMessage chatMessage) {
                log("received message: " + chatMessage);
            }

            @Override
            public void processError(QBPrivateChat privateChat, QBChatException error, QBChatMessage originMessage){
                log("processError: " + error.getLocalizedMessage());
            }

            @Override
            public void processMessageDelivered(QBPrivateChat privateChat, String messageID){
                log("message delivered: " + messageID);
            }

            @Override
            public void processMessageRead(QBPrivateChat privateChat, String messageID){
                log("message read: " + messageID);
            }
        };
    }

    private void initPrivateChatIsTypingListener(){

        // Create 1-1 chat is typing listener
        //
        privateChatIsTypingListener = new QBIsTypingListener<QBPrivateChat>() {
            @Override
            public void processUserIsTyping(QBPrivateChat qbPrivateChat) {
                log("user " + qbPrivateChat.getParticipant() + " is typing");
            }

            @Override
            public void processUserStopTyping(QBPrivateChat qbPrivateChat) {
                log("user " + qbPrivateChat.getParticipant() + " stop typing");
            }
        };
    }

    Snippet sendPrivateMessageWithText = new Snippet("send private message", "with text") {
        @Override
        public void execute() {
            if(privateChatManager == null){
                log("Please login first");
                return;
            }

            try {
                QBPrivateChat privateChat = privateChatManager.getChat(ApplicationConfig.getTestUserID2());
                if (privateChat == null) {
                    privateChat = privateChatManager.createChat(ApplicationConfig.getTestUserID2(), privateChatMessageListener);
                    privateChat.addIsTypingListener(privateChatIsTypingListener);
                }
                privateChat.sendMessage("Hey man! " + new Random().nextInt());
            } catch (XMPPException e) {
                log("send message error: " + e.getClass().getSimpleName());
            } catch (SmackException.NotConnectedException e) {
                log("send message error: " + e.getClass().getSimpleName());
            }
        }
    };

    Snippet sendPrivateMessageExtended = new Snippet("send private message", "extended") {
        @Override
        public void execute() {
            if(privateChatManager == null){
                log("Please login first");
                return;
            }

            try {

                // create a message
                QBChatMessage chatMessage = new QBChatMessage();
//                chatMessage.setBody("Hey man! " + new Random().nextInt());
                chatMessage.setProperty("name", "bob");
                chatMessage.setProperty("body", "{\"lattitude\":\"28.6156275\"}");
                chatMessage.setProperty("save_to_history", "1"); // Save to Chat 2.0 history

                chatMessage.setMarkable(true);

//                long time = System.currentTimeMillis()/1000;
//                chatMessage.setProperty("date_sent", time + "");

                // attach a photo
                QBAttachment attachment = new QBAttachment("photo");
                attachment.setId("123123");
                chatMessage.addAttachment(attachment);
                
                QBPrivateChat privateChat = privateChatManager.getChat(ApplicationConfig.getTestUserID2());
                if (privateChat == null) {
                    privateChat = privateChatManager.createChat(ApplicationConfig.getTestUserID2(), privateChatMessageListener);
                    privateChat.addIsTypingListener(privateChatIsTypingListener);
                }
                privateChat.sendMessage(chatMessage);
            } catch (XMPPException e) {
                log("send message error: " + e.getLocalizedMessage());
            } catch (SmackException.NotConnectedException e) {
                log("send message error: " + e.getClass().getSimpleName());
            }
        }
    };


    //
    ////////////////////////////////////// Typing notifications //////////////////////////////////////////
    //


    Snippet sendIsTyping = new Snippet("send is typing") {
        @Override
        public void execute() {
            if(privateChatManager == null){
                log("Please login first");
                return;
            }

            QBPrivateChat privateChat = privateChatManager.getChat(ApplicationConfig.getTestUserID2());
            if (privateChat == null) {
                privateChat = privateChatManager.createChat(ApplicationConfig.getTestUserID2(), privateChatMessageListener);
                privateChat.addIsTypingListener(privateChatIsTypingListener);
            }
            try {
                privateChat.sendIsTypingNotification();
            } catch (XMPPException e) {
                log("send typing error: " + e.getLocalizedMessage());
            } catch (SmackException.NotConnectedException e) {
                log("send typing error: " + e.getClass().getSimpleName());
            }
        }
    };

    Snippet sendStopTyping = new Snippet("send stop typing") {
        @Override
        public void execute() {
            if(privateChatManager == null){
                log("Please login first");
                return;
            }

            QBPrivateChat privateChat = privateChatManager.getChat(ApplicationConfig.getTestUserID2());
            if (privateChat == null) {
                privateChat = privateChatManager.createChat(ApplicationConfig.getTestUserID2(), privateChatMessageListener);
                privateChat.addIsTypingListener(privateChatIsTypingListener);
            }
            try {
                privateChat.sendStopTypingNotification();
            } catch (XMPPException e) {
                log("send stop typing error: " + e.getLocalizedMessage());
            } catch (SmackException.NotConnectedException e) {
                log("send stop typing error: " + e.getClass().getSimpleName());
            }
        }
    };

    Snippet readMessage = new Snippet("read message") {
        @Override
        public void execute() {
            if(privateChatManager == null){
                log("Please login first");
                return;
            }

            QBPrivateChat privateChat = privateChatManager.getChat(ApplicationConfig.getTestUserID2());
            if (privateChat == null) {
                privateChat = privateChatManager.createChat(ApplicationConfig.getTestUserID2(), privateChatMessageListener);
                privateChat.addIsTypingListener(privateChatIsTypingListener);
            }
            try {
                privateChat.readMessage(null);
            } catch (XMPPException e) {
                log("read message error: " + e.getLocalizedMessage());
            } catch (SmackException.NotConnectedException e) {
                log("read message error: " + e.getClass().getSimpleName());
            }
        }
    };


    //
    ///////////////////////////////////////////// Group Chat /////////////////////////////////////////////
    //


    private void initGroupChatMessageListener(){
        groupChatQBMessageListener = new QBMessageListener<QBGroupChat>() {
            @Override
            public void processMessage(final QBGroupChat groupChat, final QBChatMessage chatMessage) {
                log("group chat: " + groupChat.getJid() + ", processMessage: " + chatMessage.getBody());
            }

            @Override
            public void processError(final QBGroupChat groupChat, QBChatException error, QBChatMessage originMessage){

            }

            @Override
            public void processMessageDelivered(QBGroupChat groupChat, String messageID){
                // never be called, works only for 1-1 chat
            }

            @Override
            public void processMessageRead(QBGroupChat groupChat, String messageID){
                // never be called, works only for 1-1 chat
            }
        };
    }

    Snippet joinRoom = new Snippet("join Room") {
        @Override
        public void execute() {
            if(groupChatManager == null){
                log("Please login first");
                return;
            }


            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(0);

            currentChatRoom = groupChatManager.createGroupChat(ApplicationConfig.testRoomJID);

            currentChatRoom.join(history, new QBEntityCallbackImpl() {
                @Override
                public void onSuccess() {
                    log("join Room success");

                    // add listeners
                    currentChatRoom.addMessageListener(groupChatQBMessageListener);
                }

                @Override
                public void onError(final List list) {
                    log("join Room error: " + list);
                }
            });
        }
    };

    Snippet joinRoomSynchronous = new AsyncSnippet("join Room (synchronous)", context) {
        @Override
        public void executeAsync() {
            if(groupChatManager == null){
                log("Please login first");
                return;
            }


            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(10);

            currentChatRoom = groupChatManager.createGroupChat(ApplicationConfig.testRoomJID);

            try {
                currentChatRoom.join(history);

                // add listeners
                currentChatRoom.addMessageListener(groupChatQBMessageListener);
            } catch (XMPPException e) {
                setException(e);
            } catch (SmackException e) {
                setException(e);
            }
        }

        @Override
        protected void postExecute() {
            super.postExecute();

            if(groupChatManager == null){
                return;
            }

            final Exception exc = getException();

            if (exc == null) {
                log("Join room success");
            }else{
                log("Join error: " + exc.getClass().getSimpleName());
            }
        }
    };

    Snippet sendMessageToRoomWithText = new Snippet("send message to room", "with text") {
        @Override
        public void execute() {
            if(currentChatRoom == null){
                log("Please join room first");
                return;
            }

            try {
                currentChatRoom.sendMessage("Hello!");
            } catch (XMPPException e) {
                log("Send message error: " + e.getLocalizedMessage());
            } catch (SmackException.NotConnectedException e) {
                log("Send message error: " + e.getClass().getSimpleName());
            } catch (IllegalStateException e){
                log("Send message error: " + e.getLocalizedMessage());
            }
        }
    };

    Snippet sendMessageToRoomExtended = new Snippet("send message to room", "extended") {
        @Override
        public void execute() {
            if(currentChatRoom == null){
                log("Please join room first");
                return;
            }

            // create a message
            QBChatMessage chatMessage = new QBChatMessage();
            chatMessage.setBody("[USRXXKLFTY9P]");
            chatMessage.setProperty("save_to_history", "1"); // Save to Chat 2.0 history

            try {
                currentChatRoom.sendMessage(chatMessage);
            } catch (XMPPException e) {
                log("Send message error: " + e.getLocalizedMessage());
            } catch (SmackException.NotConnectedException e) {
                log("Send message error: " + e.getClass().getSimpleName());
            } catch (IllegalStateException e){
                log("Send message error: " + e.getLocalizedMessage());
            }
        }
    };

    Snippet getOnlineRoomUsersSynchronous = new Snippet("get online room users") {
        @Override
        public void execute() {
            if(currentChatRoom == null){
                log("Please join room first");
                return;
            }

            Collection<Integer> onlineRoomUsers = null;
            try {
                onlineRoomUsers = currentChatRoom.getOnlineUsers();
            } catch (XMPPException e) {
                log("get online users error: " + e.getLocalizedMessage());
            }

            String onlineUser = "online users: ";
            if (onlineRoomUsers != null) {
                for (Integer userID : onlineRoomUsers) {
                    onlineUser += userID;
                    onlineUser += ", ";
                }
            }
            log(onlineUser);
        }
    };

    Snippet leaveRoom = new AsyncSnippet("leave room", context) {
        @Override
        public void executeAsync() {
            if(currentChatRoom == null){
                log("Please join room first");
                return;
            }

            try {
                currentChatRoom.leave();
                currentChatRoom = null;
            } catch (XMPPException e) {
                setException(e);
            } catch (SmackException.NotConnectedException e) {
                setException(e);
            }
        }

        @Override
        protected void postExecute() {
            super.postExecute();

            if(currentChatRoom == null){
                return;
            }

            final Exception exc = getException();

            if (exc == null) {
                log("Leave success");
            }else{
                log("Leave error: " + exc.getClass().getSimpleName());
            }
        }
    };


    //
    ///////////////////////////////////////////// Chat_2.0 /////////////////////////////////////////////
    //


    Snippet getDialogs = new Snippet("Get Dialogs") {
        @Override
        public void execute() {

            QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
            requestBuilder.setPagesLimit(100);
//            requestBuilder.addParameter("data[class_name]", "Advert");

            QBChatService.getChatDialogs(null, requestBuilder, new QBEntityCallbackImpl<ArrayList<QBDialog>>() {
                @Override
                public void onSuccess(ArrayList<QBDialog> dialogs, Bundle args) {
                    Log.i(TAG, "dialogs: " + dialogs);
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet getDialogsSynchronous = new AsyncSnippet("Get Dialogs (synchronous)", context) {
        @Override
        public void executeAsync() {
            try {
                BaseService.createFromExistentToken("65a84b4fc2757caa8adf7191f1f6f5946479e85f", new Date());
            } catch (BaseServiceException e) {
                e.printStackTrace();
            }

            Bundle bundle = new Bundle();
            //
            QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
            requestBuilder.setPagesLimit(100);
            requestBuilder.all("occupants_ids", "76,58");
//            requestBuilder.addParameter("data[class_name]", "Advert");
            //
            List<QBDialog> chatDialogsList = null;

            try {
                chatDialogsList = QBChatService.getChatDialogs(null, requestBuilder,
                        bundle);
            }catch (QBResponseException e){
                setException(e);
            }

            if(chatDialogsList != null){
                Log.i(TAG, "chatDialogsList: " + chatDialogsList);
            }
        }
    };

    Snippet createDialog = new Snippet("Create Dialog") {
        @Override
        public void execute() {
            if(groupChatManager == null){
                log("Please login first");
                return;
            }

            QBRequestCreateBuilder additionalParams = new QBRequestCreateBuilder();
//            additionalParams.addParameter("data[class_name]", "Advert");
//            additionalParams.addParameter("data[title]", "bingo");

            ArrayList<Integer> occupantIdsList = new ArrayList<Integer>();
            occupantIdsList.add(ApplicationConfig.testUserID2);

            QBDialog dialog = new QBDialog();
            dialog.setName("Chat with Garry and John");
            dialog.setPhoto("452444");
            dialog.setType(QBDialogType.GROUP);
            dialog.setOccupantsIds(occupantIdsList);

            groupChatManager.createDialog(dialog, additionalParams, new QBEntityCallbackImpl<QBDialog>() {
                @Override
                public void onSuccess(QBDialog dialog, Bundle args) {
                    Log.i(TAG, "dialog: " + dialog);
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet createDialogSynchronous = new AsyncSnippet("Create Dialog (synchronous)", context) {
        @Override
        public void executeAsync() {
            if(groupChatManager == null){
                log("Please login first");
                return;
            }
            QBRequestCreateBuilder additionalParams = new QBRequestCreateBuilder();
//            additionalParams.addParameter("data[class_name]", "Advert");
//            additionalParams.addParameter("data[title]", "bingo");

            ArrayList<Integer> occupantIdsList = new ArrayList<Integer>();
            occupantIdsList.add(ApplicationConfig.testUserID2);
            //
            QBDialog dialog = new QBDialog();
            dialog.setName("Chat with Garry and John");
            dialog.setPhoto("452444");
            dialog.setType(QBDialogType.GROUP);
            dialog.setOccupantsIds(occupantIdsList);


            QBDialog createdDialog = null;
            try {
                createdDialog = groupChatManager.createDialog(dialog, additionalParams);
            }catch (QBResponseException e){
                setException(e);
            }

            if(createdDialog != null){
                Log.i(TAG, "dialog: " + createdDialog);
            }
        }
    };

    Snippet updateDialog = new Snippet("Update Dialog") {
        @Override
        public void execute() {
            if(groupChatManager == null){
                log("Please login first");
                return;
            }

            QBRequestUpdateBuilder requestBuilder = new QBRequestUpdateBuilder();
            requestBuilder.pullAll(com.quickblox.chat.Consts.DIALOG_OCCUPANTS, 378);
//            requestBuilder.addParameter("data[class_name]", "Advert");
//            requestBuilder.addParameter("data[title]", "bingo");

            QBDialog dialog = new QBDialog("5444bba7535c121d3302245f");
            dialog.setName("Chat with Garry and John");
            dialog.setPhoto("452444");

            groupChatManager.updateDialog(dialog, requestBuilder, new QBEntityCallbackImpl<QBDialog>() {
                @Override
                public void onSuccess(QBDialog dialog, Bundle args) {
                    Log.i(TAG, "dialog: " + dialog);
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet updateDialogSynchronous = new AsyncSnippet("Update Dialog (synchronous)", context) {
        @Override
        public void executeAsync() {

            if(groupChatManager == null){
                log("Please login first");
                return;
            }

            QBDialog dialog = new QBDialog("5444bba7535c121d3302245f");
            dialog.setName("Chat with Garry and John");
            dialog.setPhoto("452444");

            QBRequestUpdateBuilder requestBuilder = new QBRequestUpdateBuilder();
            requestBuilder.pullAll(com.quickblox.chat.Consts.DIALOG_OCCUPANTS, 378);
//            requestBuilder.addParameter("data[class_name]", "Advert");
//            requestBuilder.addParameter("data[title]", "bingo");

            QBDialog updatedDialog = null;
            try {
                updatedDialog = groupChatManager.updateDialog(dialog, requestBuilder);
            }catch (QBResponseException e){
                setException(e);
            }

            if(updatedDialog != null){
                Log.i(TAG, "dialog: " + updatedDialog);
            }
        }
    };


    Snippet deleteDialog = new Snippet("Delete Dialog") {
        @Override
        public void execute() {
            if(groupChatManager == null){
                log("Please login first");
                return;
            }

            String dialogID = "5444bba7535c121d3302245f";

            groupChatManager.deleteDialog(dialogID, new QBEntityCallbackImpl<Void>() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "dialog deleted");
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet deleteDialogSynchronous = new AsyncSnippet("Delete Dialog (synchronous)", context) {
        @Override
        public void executeAsync() {

            if(groupChatManager == null){
                log("Please login first");
                return;
            }

            String dialogID = "5444bbc7535c12e10f0233be";

            try {
                groupChatManager.deleteDialog(dialogID);
                Log.i(TAG, "dialog deleted");
            }catch (QBResponseException e){
                setException(e);
            }
        }
    };



    Snippet getMessages = new Snippet("Get Messages", "with dialog id") {
        @Override
        public void execute() {
            QBDialog qbDialog = new QBDialog("53cfc593efa3573ebd000017");

            QBRequestGetBuilder customObjectRequestBuilder = new QBRequestGetBuilder();
            customObjectRequestBuilder.setPagesLimit(100);

            QBChatService.getDialogMessages(qbDialog, customObjectRequestBuilder, new QBEntityCallbackImpl<ArrayList<QBChatHistoryMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatHistoryMessage> messages, Bundle args) {
                    Log.i(TAG, "messages\n: " + messages);
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet getMessagesSynchronous = new AsyncSnippet("Get Messages (synchronous)", "with dialog id", context) {
        @Override
        public void executeAsync() {
            Bundle bundle = new Bundle();
            //
            QBRequestGetBuilder customObjectRequestBuilder = new QBRequestGetBuilder();
            customObjectRequestBuilder.setPagesLimit(100);

            QBDialog dialog = new QBDialog("53cfc593efa3573ebd000017");

            List<QBChatHistoryMessage> dialogMessagesList = null;
            try {
                dialogMessagesList = QBChatService.getDialogMessages(dialog, null, bundle);
            }catch (QBResponseException e){
                setException(e);
            }

            if(dialogMessagesList != null){
                Log.i(TAG, "dialogMessagesList: " + dialogMessagesList);
            }
        }
    };


    Snippet markMessagesAsRead = new Snippet("Mark Messages as read") {
        @Override
        public void execute() {
            StringifyArrayList messagesIDs = new StringifyArrayList<String>();
            messagesIDs.add("53cfc62ee4b05ed6d7cf17d3");
            messagesIDs.add("53cfc62fe4b05ed6d7cf17d5");

            QBChatService.markMessagesAsRead("53cfc593efa3573ebd000017", null, new QBEntityCallbackImpl<Void>(){
                @Override
                public void onSuccess() {
                    Log.i(TAG, "read OK" );
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet markMessagesAsReadSynchronous = new AsyncSnippet("Mark Messages as read (synchronous)", context) {
        @Override
        public void executeAsync() {
            StringifyArrayList messagesIDs = new StringifyArrayList<String>();
            messagesIDs.add("53cfc62ee4b05ed6d7cf17d3");
            messagesIDs.add("53cfc62fe4b05ed6d7cf17d5");

            try {
                QBChatService.markMessagesAsRead("53cfc593efa3573ebd000017", messagesIDs);
                Log.i(TAG, "read OK" );
            }catch (QBResponseException e){
                setException(e);
            }
        }
    };

    Snippet deleteMessage = new Snippet("Delete Message") {
        @Override
        public void execute() {
            QBChatService.deleteMessage("53cfc62ee4b05ed6d7cf17d3", new QBEntityCallbackImpl<Void>(){
                @Override
                public void onSuccess() {
                    Log.i(TAG, "deleted OK");
                }

                @Override
                public void onError(List<String> errors) {
                    handleErrors(errors);
                }
            });
        }
    };

    Snippet deleteMessageSynchronous = new AsyncSnippet("Delete Message (synchronous)", context) {
        @Override
        public void executeAsync() {
            try {
                QBChatService.deleteMessage("53cfc62fe4b05ed6d7cf17d5");
                Log.i(TAG, "deleted OK" );
            }catch (QBResponseException e){
                setException(e);
            }
        }
    };


    //
    ///////////////////////////////////////////// Roster /////////////////////////////////////////////
    //

     private void initRoster() {
        сhatRoster = chatService.getRoster(QBRoster.SubscriptionMode.mutual, subscriptionListener);
        сhatRoster.addRosterListener(rosterListener);
    }

    private void initRosterListener(){
        rosterListener = new QBRosterListener() {
            @Override
            public void entriesDeleted(Collection<Integer> userIds) {
                log("entriesDeleted: " + userIds);
            }

            @Override
            public void entriesAdded(Collection<Integer> userIds) {
                log("entriesAdded: " + userIds);
            }

            @Override
            public void entriesUpdated(Collection<Integer> userIds) {
                log("entriesUpdated: " + userIds);
            }

            @Override
            public void presenceChanged(QBPresence presence) {
                log("presenceChanged: " + presence);
            }
        };
    }

    private void initSubscriptionListener(){
        subscriptionListener = new QBSubscriptionListener() {
            @Override
            public void subscriptionRequested(int userId) {
                log("subscriptionRequested: " + userId);
            }
        };
    }

    Snippet sendPresence = new Snippet("send presence") {
        @Override
        public void execute() {
            if(сhatRoster == null){
                log("Please login first");
                return;
            }

            QBPresence presence = new QBPresence(QBPresence.Type.online);
            try {
                сhatRoster.sendPresence(presence);
            } catch (SmackException.NotConnectedException e) {
                log("error: " + e.getClass().getSimpleName());
            }
        }
    };

    Snippet getRosterUsers = new Snippet("get roster users") {
        @Override
        public void execute() {
            if(сhatRoster == null){
                log("Please login first");
                return;
            }

            Collection<QBRosterEntry> entries = сhatRoster.getEntries();
            log("Roster users:  " + entries);
        }
    };

    Snippet getUserPresence = new Snippet("get user's presence") {
        @Override
        public void execute() {
            if(сhatRoster == null){
                log("Please login first");
                return;
            }

            int userID = ApplicationConfig.getTestUserID2();

            QBPresence presence = сhatRoster.getPresence(userID);
            if (presence == null) {
                log("No user in your roster");
                return;
            }
            if (presence.getType() == QBPresence.Type.online) {
                log("User " + userID + " is online");
            }else{
                log("User " + userID + " is offline");
            }
        }
    };

    Snippet addUserToRoster = new Snippet("add user to roster") {
        @Override
        public void execute() {
            int userID = ApplicationConfig.getTestUserID2();

            if (сhatRoster.contains(userID)) {
                try {
                    сhatRoster.subscribe(userID);
                } catch (SmackException.NotConnectedException e) {
                    log("error: " + e.getClass().getSimpleName());
                }
            } else {
                try {
                    сhatRoster.createEntry(userID, null);
                } catch (XMPPException e) {
                    log("error: " + e.getLocalizedMessage());
                } catch (SmackException.NotLoggedInException e) {
                    log("error: " + e.getClass().getSimpleName());
                } catch (SmackException.NotConnectedException e) {
                    log("error: " + e.getClass().getSimpleName());
                } catch (SmackException.NoResponseException e) {
                    log("error: " + e.getClass().getSimpleName());
                }
            }
        }
    };

    Snippet removeUserFromRoster = new Snippet("remove user from roster") {
        @Override
        public void execute() {
            int userID = ApplicationConfig.getTestUserID2();

            try {
                сhatRoster.unsubscribe(userID);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    };


    Snippet confirmAddRequest = new Snippet("confirm add request") {
        @Override
        public void execute() {
            int userID = ApplicationConfig.getTestUserID2();

            try {
                сhatRoster.confirmSubscription(userID);
            } catch (SmackException.NotConnectedException e) {
                log("error: " + e.getClass().getSimpleName());
            } catch (SmackException.NotLoggedInException e) {
                log("error: " + e.getClass().getSimpleName());
            } catch (XMPPException e) {
                log("error: " + e.getLocalizedMessage());
            } catch (SmackException.NoResponseException e) {
                log("error: " + e.getClass().getSimpleName());
            }
        }
    };

    Snippet rejectAddRequest = new Snippet("reject add request") {
        @Override
        public void execute() {
            int userID = ApplicationConfig.getTestUserID2();

            try {
                сhatRoster.reject(userID);
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            }
        }
    };


    //
    ///////////////////////////////////// Privacy List /////////////////////////////////////////////
    //

    private void initPrivacyListsManager(){
        privacyListsManager = chatService.getPrivacyListsManager();
    }

    private void initPrivacyListsListener(){
        privacyListListener = new QBPrivacyListListener() {
            @Override
            public void setPrivacyList(String listName, List<QBPrivacyListItem> listItem){
                log("on setPrivacyList:" + listName + ", items: " + listItem);
            }

            @Override
            public void updatedPrivacyList(String listName) {
                log("on setPrivacyList:" + listName);
            }
        };
        privacyListsManager.addPrivacyListListener(privacyListListener);
    }


    Snippet getPrivacyLists = new AsyncSnippet("get privacy lists (synchronous)", context) {
        @Override
        public void executeAsync() {
            List<QBPrivacyList> lists = null;

            try {
                lists = privacyListsManager.getPrivacyLists();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }

            if(lists != null) {
                log("privcay lists:" + lists.toString());
            }
        }
    };

    Snippet getPrivacyList = new AsyncSnippet("get privacy list (synchronous)", context) {
        @Override
        public void executeAsync() {
            QBPrivacyList list = null;

            try {
                list = privacyListsManager.getPrivacyList("public");
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }

            if(list != null) {
                log("public privacy list: " + list.toString());
            }

        }
    };

    Snippet setPrivacyList = new AsyncSnippet("set privacy list (synchronous)", context) {
        @Override
        public void executeAsync() {
            QBPrivacyList list = new QBPrivacyList();
            list.setName("public");

            ArrayList<QBPrivacyListItem> items = new ArrayList<QBPrivacyListItem>();

            QBPrivacyListItem item1 = new QBPrivacyListItem();
            item1.setAllow(false);
            item1.setType(QBPrivacyListItem.Type.USER_ID);
            item1.setValueForType(String.valueOf(ApplicationConfig.getTestUserID2()));
            items.add(item1);

            list.setItems(items);

            try {
                privacyListsManager.setPrivacyList(list);
                log("list set");
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
    };

    Snippet deletePrivacyList = new AsyncSnippet("delete privacy list (synchronous)", context) {
        @Override
        public void executeAsync() {
            try {
                privacyListsManager.deletePrivacyList("public");
                log("list deleted");
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
    };

    Snippet setDefaultPrivacyList = new AsyncSnippet("set default privacy list (synchronous)", context) {
        @Override
        public void executeAsync() {
            try {
                privacyListsManager.setPrivacyListAsDefault("public");
                log("list set as default");
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
    };



    //
    /////////////////////////////////// Group Chat (old methods) ///////////////////////////////////
    //


    Snippet createRoom = new Snippet("create room") {
        @Override
        public void execute() {
            //
            // Use 'create Dialog' request instead next code and then Join room using dialog's 'roomJid' field as a room jid

            currentChatRoom = groupChatManager.createGroupChat("football53_room", false, false);

            currentChatRoom.create( new QBEntityCallbackImpl() {
                @Override
                public void onSuccess() {
                    log("create room success");
                    currentChatRoom.addMessageListener(groupChatQBMessageListener);
                }

                @Override
                public void onError(List list) {
                    log("create room errors: " + list);
                }
            });
        }
    };

    Snippet addUsersToRoom = new Snippet("add users to Room") {
        @Override
        public void execute() {

            List<Integer> users = new ArrayList<Integer>();
            users.add(958); // user ced

            try {
                currentChatRoom.addRoomUsers(users);
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }

        }
    };

    Snippet removeUsersFromRoom = new Snippet("remove users from Room") {
        @Override
        public void execute() {
            List<Integer> users = new ArrayList<Integer>();
            users.add(958); // user ced

            try {
                currentChatRoom.removeRoomUsers(users);
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }

        }
    };

    Snippet getRoomUsers = new AsyncSnippet("get room users (synchronous)", context) {
        Collection<Integer> roomUsers = null;

        @Override
        public void executeAsync() {
            try {
                roomUsers = currentChatRoom.getRoomUserIds();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
                setException(e);
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
                setException(e);
            } catch (XMPPException e) {
                e.printStackTrace();
                setException(e);
            }
        }

        @Override
        protected void postExecute() {
            super.postExecute();
            final Exception exc = getException();

            if (exc == null) {
                log("Room users: " + roomUsers);
            }else{
                log("Room users error: " + exc.getLocalizedMessage());
            }
        }
    };

    Snippet getRoomInfo = new Snippet("get Room info") {
        @Override
        public void execute() {
            RoomInfo roomInfo = null;
            try {
                roomInfo = currentChatRoom.getInfo();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
            if(roomInfo != null) {
                log("roomInfo: " + roomInfo.getRoom() + ", " + roomInfo.isMembersOnly() + ", "  + roomInfo.isPersistent());
            }
        }
    };

    Snippet getRooms = new Snippet("get list of rooms") {
        @Override
        public void execute() {
            Collection<String> rooms = null;
            try {
                rooms = groupChatManager.getHostedRooms();
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (SmackException.NotConnectedException e) {
                e.printStackTrace();
            } catch (SmackException.NoResponseException e) {
                e.printStackTrace();
            }
            String roomList = "room list: ";
            for (String roomJID : rooms) {
                roomList += roomJID;
                roomList += ", ";
            }
            log(roomList);
        }
    };
}