package br.com.citizenlife.api;

import br.com.citizenlife.model.Alerta;
import br.com.citizenlife.model.Cidadao;
import br.com.citizenlife.model.Endereco;
import br.com.citizenlife.model.NivelRisco;
import br.com.citizenlife.model.Ocorrencia;
import br.com.citizenlife.model.OcorrenciaVia;
import br.com.citizenlife.model.PontoRota;
import br.com.citizenlife.model.Regiao;
import br.com.citizenlife.model.Rota;
import br.com.citizenlife.model.Via;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Converte objetos do model em mapas prontos para virar JSON.
 * Fica na camada de interface para o model não depender do formato da API.
 * A senha (hash) nunca é incluída.
 */
public final class Conversor {

    private Conversor() {
    }

    public static <T> List<Map<String, Object>> lista(List<T> itens, Function<T, Map<String, Object>> conversor) {
        return itens.stream().map(conversor).toList();
    }

    public static Map<String, Object> cidadao(Cidadao cidadao) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", cidadao.getId());
        mapa.put("nome", cidadao.getNome());
        mapa.put("email", cidadao.getEmail());
        mapa.put("modal", cidadao.getModal());
        mapa.put("perfil", cidadao.getDescricaoPerfil());
        mapa.put("dataCadastro", cidadao.getDataCadastro());
        return mapa;
    }

    public static Map<String, Object> regiao(Regiao regiao) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", regiao.getId());
        mapa.put("nome", regiao.getNome());
        return mapa;
    }

    public static Map<String, Object> via(Via via) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", via.getId());
        mapa.put("nome", via.getNome());
        mapa.put("regiao", via.getRegiao() == null ? null : regiao(via.getRegiao()));
        return mapa;
    }

    public static Map<String, Object> ocorrencia(Ocorrencia ocorrencia) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", ocorrencia.getId());
        mapa.put("titulo", ocorrencia.getTitulo());
        mapa.put("descricao", ocorrencia.getDescricao());
        mapa.put("tipo", ocorrencia.getTipo());
        mapa.put("gravidade", ocorrencia.getGravidade());
        mapa.put("status", ocorrencia.getStatus());
        mapa.put("ativa", ocorrencia.isAtiva());
        mapa.put("dataRegistro", ocorrencia.getDataRegistro());
        mapa.put("dataAtualizacao", ocorrencia.getDataAtualizacao());

        // O autor fica nulo quando a pessoa excluiu a conta; o relato continua valendo.
        Cidadao autor = ocorrencia.getAutor();
        if (autor == null) {
            mapa.put("autor", null);
        } else {
            Map<String, Object> dadosAutor = new LinkedHashMap<>();
            dadosAutor.put("id", autor.getId());
            dadosAutor.put("nome", autor.getNome());
            dadosAutor.put("modal", autor.getModal());
            mapa.put("autor", dadosAutor);
        }

        mapa.put("vias", lista(ocorrencia.getVias(), Conversor::ocorrenciaVia));
        return mapa;
    }

    private static Map<String, Object> ocorrenciaVia(OcorrenciaVia item) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("via", via(item.getVia()));
        mapa.put("nivelImpacto", item.getNivelImpacto());
        return mapa;
    }

    public static Map<String, Object> risco(double valor, NivelRisco nivel, boolean alerta) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("valor", valor);
        mapa.put("nivel", nivel);
        mapa.put("alerta", alerta);
        return mapa;
    }

    public static Map<String, Object> rota(Rota rota) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", rota.getId());
        mapa.put("idCidadao", rota.getIdCidadao());
        mapa.put("nome", rota.getNome());
        mapa.put("origem", ponto(rota.getOrigem()));
        mapa.put("destino", ponto(rota.getDestino()));
        mapa.put("criterio", rota.getCriterio());
        mapa.put("distanciaMetros", rota.getDistanciaMetros());
        mapa.put("dataCriacao", rota.getDataCriacao());
        mapa.put("vias", lista(rota.getVias(), Conversor::via));
        return mapa;
    }

    public static Map<String, Object> endereco(Endereco endereco) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", endereco.getId());
        mapa.put("idCidadao", endereco.getIdCidadao());
        mapa.put("apelido", endereco.getApelido());
        mapa.put("tipo", endereco.getTipo());
        mapa.put("local", ponto(endereco.getLocal()));
        mapa.put("dataCriacao", endereco.getDataCriacao());
        return mapa;
    }

    /** Versão curta da rota, usada dentro dos alertas. */
    private static Map<String, Object> rotaResumo(Rota rota) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", rota.getId());
        mapa.put("nome", rota.getNome());
        return mapa;
    }

    private static Map<String, Object> ponto(PontoRota ponto) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("descricao", ponto.descricao());
        mapa.put("x", ponto.x());
        mapa.put("y", ponto.y());
        return mapa;
    }

    public static Map<String, Object> alerta(Alerta alerta) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("ocorrencia", ocorrencia(alerta.getOcorrencia()));
        mapa.put("viasAfetadas", lista(alerta.getViasAfetadas(), Conversor::via));
        mapa.put("rotas", lista(alerta.getRotasAfetadas(), Conversor::rotaResumo));
        mapa.put("risco", risco(alerta.getRisco(), alerta.getNivel(), alerta.isAcimaDoLimite()));
        return mapa;
    }
}
