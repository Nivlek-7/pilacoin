package br.ufsm.poli.csi.tapw.pilacoin.service;

import br.ufsm.poli.csi.tapw.pilacoin.model.Bloco;
import br.ufsm.poli.csi.tapw.pilacoin.model.BlocoDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BlocoService {

    @Autowired
    private WebClient webClient;

    public BlocoDAO buscarBlocoPeloNumero(Long numBloco) {
        try {
            Mono<BlocoDAO> monoBlocoDAO = this.webClient
                    .get().uri(uriBuilder -> uriBuilder
                            .path("/bloco/")
                            .queryParam("numBloco", numBloco)
                            .build())
                    .retrieve()
                    .bodyToMono(BlocoDAO.class);

            return monoBlocoDAO.block();
        } catch (Exception e) {
            System.out.println("Mensagem da exceção: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public Bloco registrarBloco(Bloco blocoDescoberto) {
        try {
            Mono<Bloco> monoBloco = this.webClient
                    .post()
                    .uri("/bloco/")
                    .body(BodyInserters.fromValue(blocoDescoberto))
                    .retrieve()
                    .bodyToMono(Bloco.class);

            return monoBloco.block();

        } catch (Exception e) {
            System.out.println("Mensagem da exceção: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


}
