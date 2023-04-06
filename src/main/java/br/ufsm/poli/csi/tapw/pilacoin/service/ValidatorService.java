package br.ufsm.poli.csi.tapw.pilacoin.service;

import br.ufsm.poli.csi.tapw.pilacoin.model.Bloco;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaBlocoValidado;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import br.ufsm.poli.csi.tapw.pilacoin.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.Base64;

@Service
public class ValidatorService {

    @Autowired
    private WebClient webClient;

    public PilaBlocoValidado validaPilaCoin(PilaCoin pila) {

        BigInteger dificuldade = Utils.getDificuldade();

        if (dificuldade != null) {
            byte[] hash = Utils.getHash(pila);
            BigInteger numHash = new BigInteger(hash).abs();

            if (numHash.compareTo(dificuldade) < 0) {
                System.out.println("Pila válido! Nonce: " + pila.getNonce());
                KeyPair kp = Utils.readKeyPair();

                PilaBlocoValidado pbv = PilaBlocoValidado.builder()
                        .chavePublica(kp.getPublic().getEncoded())
                        .hashPilaBloco(Base64.getEncoder().encodeToString(hash))
                        .nonce(pila.getNonce())
                        .tipo("PILA")
                        .build();
                byte[] hashPBV = Utils.getHash(pbv);
                String assinatura = Utils.encrypt(kp.getPrivate(), hashPBV);
                pbv.setAssinatura(assinatura);

                try {
                    Mono<PilaBlocoValidado> monoPBV = this.webClient
                            .post()
                            .uri("/pilacoin/validaPilaOutroUsuario")
                            .body(BodyInserters.fromValue(pbv))
                            .retrieve()
                            .bodyToMono(PilaBlocoValidado.class);

                    return monoPBV.block();

                } catch (Exception e) {
                    System.out.println("Exceção ao validar o pilacoin: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }

            } else {
                System.out.println("PilaCoin inválido!");
                return null;
            }
        }
        return null;
    }

    public PilaBlocoValidado validaBloco(Bloco bloco) {

        BigInteger dificuldade = Utils.getDificuldade();

        if (dificuldade != null) {
            byte[] hash = Utils.getHash(bloco);
            BigInteger numHash = new BigInteger(hash).abs();

            if (numHash.compareTo(dificuldade) < 0) {
                System.out.println("Bloco válido! Nonce: " + bloco.getNonce());
                KeyPair kp = Utils.readKeyPair();

                PilaBlocoValidado pbv = PilaBlocoValidado.builder()
                        .chavePublica(kp.getPublic().getEncoded())
                        .hashPilaBloco(Base64.getEncoder().encodeToString(hash))
                        .nonce(bloco.getNonce())
                        .tipo("BLOCO")
                        .build();
                byte[] hashPBV = Utils.getHash(pbv);
                String assinatura = Utils.encrypt(kp.getPrivate(), hashPBV);
                pbv.setAssinatura(assinatura);

                try {
                    Mono<PilaBlocoValidado> monoPBV = this.webClient
                            .post()
                            .uri("/bloco/validaBlocoOutroUsuario")
                            .body(BodyInserters.fromValue(pbv))
                            .retrieve()
                            .bodyToMono(PilaBlocoValidado.class);

                    return monoPBV.block();

                } catch (Exception e) {
                    System.out.println("Exceção ao validar o bloco: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }

            } else {
                System.out.println("Bloco inválido!");
                return null;
            }
        }
        return null;
    }
}
