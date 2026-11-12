package br.com.citizenlife.dao;

import java.util.List;
import java.util.Optional;

/**
 * Contrato das operações CRUD que todo DAO de entidade principal implementa.
 *
 * @param <T> entidade do pacote model
 */
public interface CrudDao<T> {

    /** INSERT. Devolve a entidade com o id gerado pelo banco. */
    T inserir(T entidade);

    /** SELECT de todos os registros. */
    List<T> listar();

    /** SELECT por chave primária. Vazio quando o id não existe. */
    Optional<T> buscarPorId(long id);

    /** UPDATE. Devolve falso quando nenhum registro tem o id informado. */
    boolean atualizar(T entidade);

    /** DELETE. Devolve falso quando nenhum registro tem o id informado. */
    boolean excluir(long id);
}
