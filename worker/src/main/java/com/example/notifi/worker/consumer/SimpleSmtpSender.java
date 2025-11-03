package com.example.notifi.worker.consumer;

import com.example.notifi.common.messaging.NotificationTaskMessage; // SMTP sender consumes API-produced DTO
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SimpleSmtpSender implements TaskConsumer.SmtpSender {
    private final Session session;
    private final String username;
    private final String password;

    public SimpleSmtpSender(
        @Value("${smtp.host:localhost}") String host,
        @Value("${smtp.port:1025}") int port,
        @Value("${smtp.user:}") String username,
        @Value("${smtp.pass:}") String password) {
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", username != null && !username.isBlank());
        this.session = Session.getInstance(props);
        this.username = username;
        this.password = password;
    }

    @Override
    public void send(NotificationTaskMessage message) throws Exception {
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(message.recipient()));
        mimeMessage.setSubject(message.subject() != null ? message.subject() : message.templateCode());
        mimeMessage.setText("Notification " + message.notificationId());
        if (username != null && !username.isBlank()) {
            Transport.send(mimeMessage, username, password);
        } else {
            Transport.send(mimeMessage);
        }
    }
}
