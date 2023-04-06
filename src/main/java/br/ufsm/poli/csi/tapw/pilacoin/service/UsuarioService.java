package br.ufsm.poli.csi.tapw.pilacoin.service;

import br.ufsm.poli.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.poli.csi.tapw.pilacoin.repository.UsuarioRepository;
import br.ufsm.poli.csi.tapw.pilacoin.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Base64;

@Service
public class UsuarioService {

    @Autowired
    private WebClient webClient;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostConstruct
    public void init() {
        System.out.println("Usuário já cadastrado: " + cadastrarUsuario("Kelvin"));
    }

    public Usuario cadastrarUsuario(String nome) {
        // getKeyBytes verifica se já tem o par de chaves no sistema, caso não tenha cria um par
        byte[] pubKeyBytes = Utils.getKeyBytes("public_key.der");

        try { // verifica se o usuário já está cadastro
            String strPubKey = Base64.getEncoder().encodeToString(pubKeyBytes);
            Usuario retUsuario = buscaUsuario(strPubKey);

            assert retUsuario != null;
            usuarioRepository.save(retUsuario);

            return retUsuario;
        } catch (Exception e) { // caso não esteja, faz o cadastro do mesmo
            Usuario usuario = Usuario.builder().nome(nome).chavePublica(pubKeyBytes).build();
            Mono<Usuario> monoUsuario = this.webClient
                    .post()
                    .uri("/usuario/")
                    .body(BodyInserters.fromValue(usuario))
                    .retrieve()
                    .bodyToMono(Usuario.class);
            Usuario retUsuario = monoUsuario.block();
            assert retUsuario != null;
            usuarioRepository.save(retUsuario);

            return retUsuario;
        }
    }

    public Usuario buscaUsuario(String strPubKey) {
        try {
            Mono<Usuario> monoUsuario = this.webClient
                    .post()
                    .uri("/usuario/findByChave")
                    .body(BodyInserters.fromValue(strPubKey))
                    .retrieve()
                    .bodyToMono(Usuario.class);

            return monoUsuario.block();
        } catch (Exception e) {
            System.out.println("Mensagem: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
