package com.uci.adapter.netcore.whatsapp;

import com.uci.adapter.netcore.whatsapp.inbound.NetcoreWhatsAppMessage;
import com.uci.adapter.netcore.whatsapp.outbound.OutboundMessage;
import com.uci.adapter.netcore.whatsapp.outbound.SendMessageResponse;
import com.uci.adapter.netcore.whatsapp.outbound.SingleMessage;
import com.uci.adapter.netcore.whatsapp.outbound.Text;
import com.uci.adapter.provider.factory.AbstractProvider;
import com.uci.adapter.provider.factory.IProvider;
import com.uci.utils.BotService;
import io.fusionauth.domain.Application;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import messagerosa.core.model.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.function.Function;

@Slf4j
@Getter
@Setter
@Builder
public class NetcoreWhatsappAdapter extends AbstractProvider implements IProvider {

    private final static String GUPSHUP_OUTBOUND = "https://media.smsgupshup.com/GatewayAPI/rest";

    @Autowired
    @Qualifier("rest")
    private RestTemplate restTemplate;

    private BotService botservice;

    @Override
    public Mono<XMessage> convertMessageToXMsg(Object msg) {
        NetcoreWhatsAppMessage message = (NetcoreWhatsAppMessage) msg;
        SenderReceiverInfo from = SenderReceiverInfo.builder().build();
        SenderReceiverInfo to = SenderReceiverInfo.builder().userID("admin").build();

        XMessage.MessageState messageState;
        messageState = XMessage.MessageState.REPLIED;
        MessageId messageIdentifier = MessageId.builder().build();
        XMessage.MessageType messageType= XMessage.MessageType.TEXT;
        XMessagePayload xmsgPayload = XMessagePayload.builder().build();
        String appName = "";
        log.info("test");
        if (message.getEventType() != null) {
            xmsgPayload.setText("");
            messageIdentifier.setChannelMessageId(message.getMessageId());
            from.setUserID(message.getMobile().substring(2));
            XMessage.MessageState messageState1;
            String eventType = message.getEventType().toUpperCase();
            messageState1 = getMessageState(eventType);
            return Mono.just(processedXMessage(message, xmsgPayload, to, from,  messageState1, messageIdentifier,messageType));

        } else if (message.getType().equalsIgnoreCase("text")) {
            //Actual Message with payload (user response)
            messageState = XMessage.MessageState.REPLIED;
            from.setUserID(message.getMobile().substring(2));

            XMessage.MessageState finalMessageState = messageState;
            messageIdentifier.setReplyId(message.getReplyId());
            xmsgPayload.setText(message.getText().getText());

            messageIdentifier.setChannelMessageId(message.getMessageId());

            return Mono.just(processedXMessage(message, xmsgPayload, to, from, finalMessageState, messageIdentifier,messageType));
        } else if (message.getType().equals("button")) {
            from.setUserID(message.getMobile().substring(2));
            // Get the last message sent to this user using the reply-messageID
            // Get the app from that message
            // Get the buttonLinkedApp
            // Add the starting text as the first message.
            Application application = botservice.getButtonLinkedApp(appName);
            xmsgPayload.setText((String) application.data.get("startingMessage"));
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState, messageIdentifier,messageType));

        } else {
            System.out.println("No Match for parsing");
            return Mono.just(processedXMessage(message, xmsgPayload, to, from, messageState, messageIdentifier,messageType));

        }

    }

    @NotNull
    public static XMessage.MessageState getMessageState(String eventType) {
        XMessage.MessageState messageState;
        switch (eventType) {
            case "SENT":
                messageState = XMessage.MessageState.SENT;
                break;
            case "DELIVERED":
                messageState = XMessage.MessageState.DELIVERED;
                break;
            case "READ":
                messageState = XMessage.MessageState.READ;
                break;
            default:
                messageState = XMessage.MessageState.FAILED_TO_DELIVER;
                //TODO: Save the state of message and reason in this case.
                break;
        }
        return messageState;
    }

    private XMessage processedXMessage(NetcoreWhatsAppMessage message, XMessagePayload xmsgPayload, SenderReceiverInfo to,
                                       SenderReceiverInfo from, XMessage.MessageState messageState,
                                       MessageId messageIdentifier, XMessage.MessageType messageType) {
        if (message.getLocation() != null) xmsgPayload.setText(message.getLocation());
        return XMessage.builder()
                .to(to)
                .from(from)
                .channelURI("WhatsApp")
                .providerURI("Netcore")
                .messageState(messageState)
                .messageId(messageIdentifier)
                .messageType(messageType)
                .timestamp(getTimestamp(message.getEventType(), message.getTimestamp()))
                .payload(xmsgPayload).build();
    }

    Long getTimestamp(String eventType, String timestamp) {
        return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp) * 1000;
//        if (eventType != null)
//            return timestamp == null ? Timestamp.valueOf(LocalDateTime.now()).getTime() : Long.parseLong(timestamp)*1000;
//        else{
//            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            LocalDateTime date = LocalDateTime.parse(timestamp, formatter);
//            return Timestamp.valueOf(date).getTime();
//        }
    }

    @Override
    public void processOutBoundMessage(XMessage nextMsg) throws Exception {
        log.info("nextXmsg {}", nextMsg.toXML());
        callOutBoundAPI(nextMsg);
    }

    @Override
    public Mono<XMessage> processOutBoundMessageF(XMessage xMsg) {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
        String phoneNo = "91" +xMsg.getTo().getUserID();
        String text = "";

//        phoneNo = "91" + xMsg.getTo().getUserID();

//        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {
//            //no implementation
//        } else
        if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
            // OPT in user
            text = xMsg.getPayload().getText() + renderMessageChoices(xMsg.getPayload().getButtonChoices());
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());
        }
//            else {
//            //no implementation
//        }

        // SendMessage
        Text t = Text.builder().content(text).previewURL("false").build();
        Text[] texts = {t};
        return NewNetcoreService.getInstance(new NWCredentials(token)).
                sendOutboundMessage(OutboundMessage.builder().message(new SingleMessage[]{SingleMessage
                        .builder()
                        .from("461089f9-1000-4211-b182-c7f0291f3d45")
                        .to(phoneNo)
                        .recipientType("individual")
                        .messageType("text")
                        .header("custom_data")
                        .text(texts)
                        .build()}).build()).map(new Function<SendMessageResponse, XMessage>() {
            @Override
            public XMessage apply(SendMessageResponse sendMessageResponse) {
                if(sendMessageResponse != null){
                    xMsg.setMessageId(MessageId.builder().channelMessageId(sendMessageResponse.getData().getIdentifier()).build());
                    xMsg.setMessageState(XMessage.MessageState.SENT);
                }
                return xMsg;
            }
        });


    }

    private String renderMessageChoices(ArrayList<ButtonChoice> buttonChoices) {
        StringBuilder processedChoicesBuilder = new StringBuilder("");
        if(buttonChoices != null){
            for(ButtonChoice choice:buttonChoices){
                processedChoicesBuilder.append(choice.getText()).append("\n");
            }
            String processedChoices = processedChoicesBuilder.toString();
            return processedChoices.substring(0,processedChoices.length()-1);
        }
        return "";
    }

    public XMessage callOutBoundAPI(XMessage xMsg) throws Exception {
        log.info("next question to user is {}", xMsg.toXML());
        // String url = "http://federation-service:9999/admin/v1/adapter/getCredentials/" + xMsg.getAdapterId();
        // NWCredentials credentials = restTemplate.getForObject(url, NWCredentials.class);

        String phoneNo = "";
        String text = "";

        phoneNo = "91" + xMsg.getTo().getUserID();

        if (xMsg.getMessageState().equals(XMessage.MessageState.OPTED_IN)) {

        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else if (xMsg.getMessageType() != null && xMsg.getMessageType().equals(XMessage.MessageType.HSM_WITH_BUTTON)) {
            // OPT in user
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else if (xMsg.getMessageState().equals(XMessage.MessageState.REPLIED)) {
            text = xMsg.getPayload().getText()+ renderMessageChoices(xMsg.getPayload().getButtonChoices());;
        } else {
        }

        // SendMessage
        Text t = Text.builder().content(text).previewURL("false").build();
        Text[] texts = {t};

        SingleMessage msg = SingleMessage
                .builder()
                .from("461089f9-1000-4211-b182-c7f0291f3d45")
                .to(phoneNo)
                .recipientType("individual")
                .messageType("text")
                .header("custom_data")
                .text(texts)
                .build();
        SingleMessage[] messages = {msg};

        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJuZXRjb3Jlc2FsZXNleHAiLCJleHAiOjI0MjUxMDI1MjZ9.ljC4Tvgz031i6DsKr2ILgCJsc9C_hxdo2Kw8iZp9tsVcCaKbIOXaFoXmpU7Yo7ob4P6fBtNtdNBQv_NSMq_Q8w";
        NWCredentials nc = NWCredentials.builder().build();
        nc.setToken(token);
        NetcoreService ns = new NetcoreService(nc);

        OutboundMessage outboundMessage = OutboundMessage.builder().message(messages).build();
        SendMessageResponse response = ns.sendText(outboundMessage);

        xMsg.setMessageId(MessageId.builder().channelMessageId(response.getData().getIdentifier()).build());
        xMsg.setMessageState(XMessage.MessageState.SENT);


        return xMsg;
    }

}