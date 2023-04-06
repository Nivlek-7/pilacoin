package br.ufsm.poli.csi.tapw.pilacoin.util;

import br.ufsm.poli.csi.tapw.pilacoin.model.Bloco;
import br.ufsm.poli.csi.tapw.pilacoin.model.BlocoDAO;
import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import br.ufsm.poli.csi.tapw.pilacoin.service.BlocoService;
import br.ufsm.poli.csi.tapw.pilacoin.service.PilaCoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Random;

@Service
public class Mineracao {

    @Autowired
    PilaCoinService pilaCoinService;

    @Autowired
    BlocoService blocoService;

    @PostConstruct
    private void init() {
        //new Thread(new MineradorPilaCoin()).start();
    }

    public void mineradorBloco (BlocoDAO blocoDAO) {
        byte[] chavePublica = Utils.getKeyBytes("public_key.der");

        Bloco blocoDescoberto = Bloco.builder()
                .chaveUsuarioMinerador(chavePublica)
                .numeroBloco(blocoDAO.getNumeroBloco())
                .transacoes(blocoDAO.getTransacoes())
                .build();

        while (true) {
            BigInteger dificuldade = Utils.getDificuldade();

            if (dificuldade != null) {
                Random rnd = new SecureRandom();
                blocoDescoberto.setNonce(String.valueOf(new BigInteger(128, rnd).abs()));

                BigInteger numHash = new BigInteger(Utils.getHash(blocoDescoberto)).abs();

                if (numHash.compareTo(dificuldade) < 0) {
                    Bloco blocoRet = blocoService.registrarBloco(blocoDescoberto);
                    System.out.println("Número do bloco: "+ blocoRet.getNumeroBloco() + ", Nonce do bloco registrado: " + blocoRet.getNonce());
                    break;
                }
            }
        }


    }

    private class MineradorPilaCoin implements Runnable {
        byte[] pubKeyBytes = Utils.getKeyBytes("public_key.der");
        int controle = 1;
        int quantidadePraMinerar = 4;

        public void run() {
            while (true) {
                BigInteger dificuldade = Utils.getDificuldade();

                if (dificuldade != null) {
                    Random rnd = new SecureRandom();
                    PilaCoin pilaCoin = PilaCoin.builder()
                            .dataCriacao(new Date())
                            .chaveCriador(pubKeyBytes)
                            .nonce(String.valueOf(new BigInteger(128, rnd).abs()))
                            .build();

                    BigInteger numHash = new BigInteger(Utils.getHash(pilaCoin)).abs();

                    if (numHash.compareTo(dificuldade) < 0) {  // compara com a dificuldade(numero fornecido pelo o professor)
                        System.out.println("Número mágico: " + pilaCoin.getNonce() +  " Data: " + pilaCoin.getDataCriacao());
                        PilaCoin pilaRetorno = pilaCoinService.registraPilaCoin(pilaCoin);
                        System.out.println("Nonce pila retornado: " + pilaRetorno.getNonce());
                        System.out.println(controle + " PilaCoin(s) Minerado");

                        if (controle < quantidadePraMinerar) {
                            controle++;
                        } else {
                            break;
                        }

                    }
                }
            }
        }
    }
}
