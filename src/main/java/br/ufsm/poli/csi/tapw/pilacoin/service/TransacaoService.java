package br.ufsm.poli.csi.tapw.pilacoin.service;

import br.ufsm.poli.csi.tapw.pilacoin.model.Log;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import br.ufsm.poli.csi.tapw.pilacoin.model.Transacao;
import br.ufsm.poli.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.poli.csi.tapw.pilacoin.repository.LogRepository;
import br.ufsm.poli.csi.tapw.pilacoin.repository.PilaCoinRepository;
import br.ufsm.poli.csi.tapw.pilacoin.repository.TransacaoRepository;
import br.ufsm.poli.csi.tapw.pilacoin.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Date;

@Service
public class TransacaoService {

    @Autowired
    private WebClient webClient;

    @Autowired
    TransacaoRepository transacaoRepository;

    @Autowired
    PilaCoinRepository pilaCoinRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UsuarioService usuarioService;

    @PostConstruct
    private void init() {
        String chaveUsuarioDestino = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAoImqklUVCQHJJqXbopAHN9fXsDIAvbt4f9DhadSvVCcZaPfyguill7q6zENkSQHzajPV4O7TYGm9ly9mrpUbbEekhFkesfkCCh0QNSz9ifKoYay9KDwhrKhORpGnTq1U6rRGsRy+lQMuknWdH4q6t48Q3KMvsdISVXC1f3m8jsbjXa6X8p/5w9BRoAVYRnLZopp/cLDUxaJ8w5QNfXaod++OGMv7OHe2bB572FCYS4YqHa2X/KYsmM2BFunkS4TO6wR/8mYC0FJkKMrwS8YszNEKsNSmdOy8IgBCmm+LVESEq+mAqstbomVNQ4qynmPwR2Np1Qv6pu//RBw7YPf8gwIDAQAB";
        String noncePila = "";

        //realizarTransferencia(chaveUsuarioDestino, noncePila);
    }
    public void realizarTransferencia(String chaveUsuarioDestinoString, String noncePila) {
        KeyPair kp = Utils.readKeyPair();
        byte[] chaveUsuarioDestino = Base64.getDecoder().decode(chaveUsuarioDestinoString);
        Usuario destinatario = usuarioService.buscaUsuario(chaveUsuarioDestinoString);
        Usuario remetente = usuarioService.buscaUsuario(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));

        Transacao transacao = Transacao.builder()
                .chaveUsuarioDestino(chaveUsuarioDestino)
                .chaveUsuarioOrigem(kp.getPublic().getEncoded())
                .dataTransacao(new Date())
                .noncePila(noncePila)
                .build();

        byte[] hash = Utils.getHash(transacao);
        String assinatura = Utils.encrypt(kp.getPrivate(), hash);

        transacao.setAssinatura(assinatura);

        try {
            Mono<Transacao> monoTransacao = this.webClient
                    .post()
                    .uri("/pilacoin/transfere")
                    .body(BodyInserters.fromValue(transacao))
                    .retrieve()
                    .bodyToMono(Transacao.class);

            Transacao retTransacao = monoTransacao.block();
            assert retTransacao != null;
            transacaoRepository.save(retTransacao);
            PilaCoin pilaExclusao = pilaCoinRepository.getPilaCoinByNonce(retTransacao.getNoncePila());
            pilaCoinRepository.delete(pilaExclusao);
            System.out.println("Transação realizada com sucesso");
            logRepository.save(new Log("[TRANSAÇÃO] " + remetente.getNome() + " transferiu com sucesso 1 pilacoin para "
                    + destinatario.getNome()+", Nonce: " + retTransacao.getNoncePila()));

        } catch (Exception e) {
            logRepository.save(new Log("[TRANSAÇÃO] " + remetente.getNome() + " obteve falha ao transferir 1 pilacoin para "
                    + destinatario.getNome()+"."));
            System.out.println("Mensagem da exceção: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
