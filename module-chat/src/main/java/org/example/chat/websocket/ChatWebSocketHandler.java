package org.example.chat.websocket;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.chat.service.ChatService;
import org.example.chat.service.ChatService.ChatSendRequest;
import org.example.chat.service.ChatService.ChatSendResponse;
import org.example.chat.service.ChatService.SelectedSong;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final long DEFAULT_USER_ID = 1L;

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                new ChatSocketResponse("connected", "DJ WebSocket 已连接", List.of(), List.of(), null)
        )));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatSocketRequest request = objectMapper.readValue(message.getPayload(), ChatSocketRequest.class);
            if (!"message".equals(request.type())) {
                sendError(session, "暂不支持的消息类型");
                return;
            }

            Long userId = request.userId() == null ? DEFAULT_USER_ID : request.userId();
            ChatSendResponse response = chatService.send(new ChatSendRequest(userId, request.content(), request.city()));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                    new ChatSocketResponse(
                            "reply",
                            response.reply().text(),
                            response.songs(),
                            response.selectedSongs(),
                            response.reply().time()
                    )
            )));
        } catch (Exception e) {
            sendError(session, "消息格式有误，请重新发送");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // No server-side session state is required at this stage.
    }

    private void sendError(WebSocketSession session, String content) throws IOException {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                new ChatSocketResponse("error", content, List.of(), List.of(), null)
        )));
    }

    public record ChatSocketRequest(String type, Long userId, String content, String city) {
    }

    public record ChatSocketResponse(
            String type,
            String content,
            List<String> songs,
            List<SelectedSong> selectedSongs,
            String time) {
    }
}
