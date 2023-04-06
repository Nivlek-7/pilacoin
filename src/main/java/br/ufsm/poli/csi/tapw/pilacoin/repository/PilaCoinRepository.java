package br.ufsm.poli.csi.tapw.pilacoin.repository;

import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PilaCoinRepository extends JpaRepository<PilaCoin, Long> {

    PilaCoin getPilaCoinByNonce(String nonce);

    List<PilaCoin> getAllByChaveCriador(byte[] chaveCriador);
}
