package br.com.citizenroute.api;

import br.com.citizenroute.model.Acao;
import br.com.citizenroute.model.Alerta;
import br.com.citizenroute.model.Cidadao;
import br.com.citizenroute.model.Gestor;
import br.com.citizenroute.model.NivelRisco;
import br.com.citizenroute.model.Ocorrencia;
import br.com.citizenroute.model.OcorrenciaVia;
import br.com.citizenroute.model.PontoRota;
import br.com.citizenroute.model.Regiao;
import br.com.citizenroute.model.Rota;
import br.com.citizenroute.model.Usuario;
import br.com.citizenroute.model.Via;

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

    public static Map<String, Object> usuario(Usuario usuario) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", usuario.getId());
        mapa.put("nome", usuario.getNome());
        mapa.put("email", usuario.getEmail());
        mapa.put("tipo", usuario.getTipo());
        mapa.put("perfil", usuario.getDescricaoPerfil());
        mapa.put("dataCadastro", usuario.getDataCadastro());
        if (usuario instanceof Cidadao cidadao) {
            mapa.put("modal", cidadao.getModal());
        } else if (usuario instanceof Gestor gestor) {
            mapa.put("orgao", gestor.getOrgao());
            mapa.put("matricula", gestor.getMatricula());
        }
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
        mapa.put("orgaoResponsavel", ocorrencia.getTipo().getOrgaoResponsavel());
        mapa.put("dataRegistro", ocorrencia.getDataRegistro());
        mapa.put("dataAtualizacao", ocorrencia.getDataAtualizacao());

        Map<String, Object> autor = new LinkedHashMap<>();
        autor.put("id", ocorrencia.getAutor().getId());
        autor.put("nome", ocorrencia.getAutor().getNome());
        autor.put("modal", ocorrencia.getAutor().getModal());
        mapa.put("autor", autor);

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

    public static Map<String, Object> acao(Acao acao) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", acao.getId());
        mapa.put("idOcorrencia", acao.getIdOcorrencia());
        mapa.put("descricao", acao.getDescricao());
        mapa.put("statusNovo", acao.getStatusNovo());
        mapa.put("dataAcao", acao.getDataAcao());

        Map<String, Object> gestor = new LinkedHashMap<>();
        gestor.put("id", acao.getGestor().getId());
        gestor.put("nome", acao.getGestor().getNome());
        gestor.put("orgao", acao.getGestor().getOrgao());
        mapa.put("gestor", gestor);
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
