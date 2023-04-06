package br.ufsm.poli.csi.tapw.pilacoin.service;

import br.ufsm.poli.csi.tapw.pilacoin.model.Bloco;
import br.ufsm.poli.csi.tapw.pilacoin.model.BlocoDAO;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaBlocoValidado;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import br.ufsm.poli.csi.tapw.pilacoin.util.Mineracao;
import br.ufsm.poli.csi.tapw.pilacoin.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.Objects;

@Service
public class WebSocketClientService {

    @Autowired
    private ValidatorService validatorService;

    @Autowired
    private BlocoService blocoService;

    @Autowired
    private Mineracao minerService;

    private MyStompSessionHandler sessionHandler;
    @Value("${endereco.server}")
    private String enderecoServer;

    @PostConstruct
    private void init() {
        sessionHandler = new MyStompSessionHandler(validatorService, blocoService, minerService);
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.connect("ws://" + enderecoServer + "/websocket/websocket", sessionHandler);
    }

    public BigInteger getDificuldade() {
        return sessionHandler.dificuldade;
    }

    //@Scheduled(fixedRate = 3000)
    private void printDificuldade() {
        if (sessionHandler.dificuldade != null) {
            System.out.println("Dificuldade Atual: " + sessionHandler.dificuldade);
        }
    }

    private static class MyStompSessionHandler implements StompSessionHandler {

        private final ValidatorService validatorService;
        private final BlocoService blocoService;
        private final Mineracao minerService;
        private BigInteger dificuldade;

        private MyStompSessionHandler(ValidatorService validatorService, BlocoService blocoService, Mineracao minerService) {
            this.validatorService = validatorService;
            this.blocoService = blocoService;
            this.minerService = minerService;
        }

        @Override
        public void afterConnected(StompSession stompSession, StompHeaders stompHeaders) {
            stompSession.subscribe("/topic/dificuldade", this);
            //stompSession.subscribe("/topic/validaMineracao", this);
            //stompSession.subscribe("/topic/descobrirNovoBloco", this);
            //stompSession.subscribe("/topic/validaBloco", this);
        }

        @Override
        public void handleException(StompSession stompSession, StompCommand stompCommand, StompHeaders stompHeaders,
                                    byte[] bytes, Throwable throwable) {
        }

        @Override
        public void handleTransportError(StompSession stompSession, Throwable throwable) {
        }

        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            if (Objects.equals(stompHeaders.getDestination(), "/topic/dificuldade")) {
                return DificuldadeRet.class;
            } else if (Objects.equals(stompHeaders.getDestination(), "/topic/validaMineracao")) {
                return PilaCoin.class;
            } else if (Objects.equals(stompHeaders.getDestination(), "/topic/descobrirNovoBloco")) {
                System.out.println("Chegou um numero de bloco");
                return NumeroBlocoRet.class;
            } else if (Objects.equals(stompHeaders.getDestination(), "/topic/validaBloco")) {
                return Bloco.class;
            }

            return null;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            assert o != null;
            if (Objects.equals(stompHeaders.getDestination(), "/topic/dificuldade")) {
                Utils.setDificuldade(new BigInteger(((DificuldadeRet) o).getDificuldade(), 16));

            } else if (Objects.equals(stompHeaders.getDestination(), "/topic/validaMineracao")) {
                PilaCoin pilaCoin = (PilaCoin) o;
                PilaBlocoValidado pbv = validatorService.validaPilaCoin(pilaCoin);

            } else if (Objects.equals(stompHeaders.getDestination(), "/topic/descobrirNovoBloco")) {
                NumeroBlocoRet ret = (NumeroBlocoRet) o;
                BlocoDAO blocoDAO = blocoService.buscarBlocoPeloNumero(ret.getNumeroBloco());
                minerService.mineradorBloco(blocoDAO);

            } else if (Objects.equals(stompHeaders.getDestination(), "/topic/validaBloco")) {
                Bloco bloco = (Bloco) o;
                System.out.println("Numero bloco " + bloco.getNumeroBloco());
                PilaBlocoValidado pbv = validatorService.validaBloco(bloco);
            }
        }

    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DificuldadeRet {
        private String dificuldade;
    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class NumeroBlocoRet {
        private Long numeroBloco;
    }
}
