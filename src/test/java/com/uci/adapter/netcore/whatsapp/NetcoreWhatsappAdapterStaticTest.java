package com.uci.adapter.netcore.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uci.utils.BotService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;

@Slf4j
@ExtendWith(MockitoExtension.class)
class NetcoreWhatsappAdapterStaticTest {

    NetcoreWhatsappAdapter adapter;
    ObjectMapper objectMapper;
    String simplePayload, readPayload, sentPayload, deliveredPayload;

    @Mock
    BotService botService;

    @SneakyThrows
    @BeforeEach
    public void init() {

        objectMapper = new ObjectMapper();
        adapter = NetcoreWhatsappAdapter
                .builder()
                .botservice(botService)
                .build();
    }

    @Test
    public void timestampParsingREAD(){
        Long timestamp = adapter.getTimestamp("READ", "2019-05-16 15:36:58");
        assertEquals(Long.parseLong("1558001218000"), timestamp);
    }

    @Test
    public void timestampParsingREPLIED(){
        Long timestamp = adapter.getTimestamp(null, "1567090835");
        System.out.println(timestamp);
        assertEquals(Long.parseLong("1567090835000"), timestamp);
    }

    @AfterAll
    static void teardown() {
        System.out.println("Teardown");
    }

}
