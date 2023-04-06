package br.ufsm.poli.csi.tapw.pilacoin.service;

import br.ufsm.poli.csi.tapw.pilacoin.model.Log;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import br.ufsm.poli.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.poli.csi.tapw.pilacoin.repository.LogRepository;
import br.ufsm.poli.csi.tapw.pilacoin.repository.PilaCoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.transaction.Transactional;
import java.util.Base64;
import java.util.List;

@Service
public class PilaCoinService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private PilaCoinRepository pilaCoinRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Transactional
    public PilaCoin registraPilaCoin(PilaCoin pilaCoin) {
        Usuario usuario = usuarioService.buscaUsuario(Base64.getEncoder().encodeToString(pilaCoin.getChaveCriador()));

        try {
            Mono<PilaCoin> monoPilaCoin = this.webClient
                    .post()
                    .uri("/pilacoin/")
                    .body(BodyInserters.fromValue(pilaCoin))
                    .retrieve()
                    .bodyToMono(PilaCoin.class);

            PilaCoin retPilaCoin = monoPilaCoin.block();
            assert retPilaCoin != null;
            pilaCoinRepository.save(retPilaCoin);
            logRepository.save(new Log("[MINERAÇÃO] Usuário " + usuario.getNome() + " minerou com sucesso 1 PilaCoin, nonce: " + retPilaCoin.getNonce() + "."));
            return retPilaCoin;

        } catch (Exception e) {
            logRepository.save(new Log("[MINERAÇÃO] Usuário " + usuario.getNome() + "obteve falha ao minerar PilaCoin"));
            System.out.println("Mensagem da exceção: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<PilaCoin> buscaPilacoinUsuario(byte[] chaveCriador) {
        return pilaCoinRepository.getAllByChaveCriador(chaveCriador);
    }

}
