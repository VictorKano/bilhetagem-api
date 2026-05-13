package com.bilhetagem.cobranca.repository;

import com.bilhetagem.cobranca.domain.Cobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    /**
     * Busca a cobrança mais recente por txid, ordenada pelo id descendente.
     * Utilizada no processamento do webhook PIX para localizar a versão mais atual.
     *
     * @param txid identificador da transação PIX
     * @return a cobrança mais recente com o txid informado, se existir
     */
    Optional<Cobranca> findTopByTxidOrderByIdDesc(String txid);

    /**
     * Busca cobrança pelo transactionId.
     * Utilizada na validação 3DS para localizar a cobrança de cartão de crédito.
     *
     * @param transactionId identificador da transação de cartão
     * @return a cobrança com o transactionId informado, se existir
     */
    Optional<Cobranca> findByTransactionId(String transactionId);
}
