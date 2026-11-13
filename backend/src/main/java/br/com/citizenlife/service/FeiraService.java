package br.com.citizenlife.service;

import br.com.citizenlife.dao.FeiraDao;
import br.com.citizenlife.exception.CitizenLifeException;
import br.com.citizenlife.exception.ValidacaoException;
import br.com.citizenlife.model.Feira;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Consulta de feiras livres e feiras favoritas do cidadão. */
public class FeiraService {

    private static final Set<String> ZONAS = Set.of("Centro", "Zona Norte", "Zona Sul", "Zona Leste", "Zona Oeste");
    private static final Set<String> DIAS = Set.of("SEGUNDA", "TERCA", "QUARTA", "QUINTA", "SEXTA", "SABADO", "DOMINGO");

    private final FeiraDao feiraDao;
    private final CidadaoService cidadaoService;

    public FeiraService(FeiraDao feiraDao, CidadaoService cidadaoService) {
        this.feiraDao = feiraDao;
        this.cidadaoService = cidadaoService;
    }

    public List<Feira> listar(String zona, String dia) {
        String zonaValida = normalizarZona(zona);
        String diaValido = normalizarDia(dia);
        return feiraDao.listarPorZonaEDia(zonaValida, diaValido);
    }

    public List<Feira> listarFavoritas(long idCidadao, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        return feiraDao.listarFavoritasPorCidadao(idCidadao);
    }

    public Feira favoritar(long idCidadao, long idFeira, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        Feira feira = feiraDao.buscarPorId(idFeira)
                .orElseThrow(() -> CitizenLifeException.naoEncontrado("Feira não encontrada."));
        feiraDao.favoritar(idCidadao, idFeira);
        return feira;
    }

    public void desfavoritar(long idCidadao, long idFeira, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        boolean removida = feiraDao.desfavoritar(idCidadao, idFeira);
        if (!removida) {
            throw CitizenLifeException.naoEncontrado("Esta feira não está na sua lista de favoritas.");
        }
    }

    public Feira hoje(long idCidadao, Long idAtor) {
        cidadaoService.exigirDono(idAtor, idCidadao);
        return feiraDao.buscarHojePorCidadao(idCidadao, diaDeHoje()).orElse(null);
    }

    private String normalizarZona(String zona) {
        if (zona == null || zona.isBlank()) {
            return null;
        }
        String valor = zona.trim();
        if (!ZONAS.contains(valor)) {
            throw new ValidacaoException("zona", "Zona inválida. Escolha uma zona da cidade.");
        }
        return valor;
    }

    private String normalizarDia(String dia) {
        if (dia == null || dia.isBlank()) {
            return null;
        }
        String valor = dia.trim().toUpperCase(Locale.ROOT);
        if (!DIAS.contains(valor)) {
            throw new ValidacaoException("dia", "Dia inválido. Escolha um dia da semana.");
        }
        return valor;
    }

    private String diaDeHoje() {
        return switch (LocalDate.now().getDayOfWeek()) {
            case MONDAY -> "SEGUNDA";
            case TUESDAY -> "TERCA";
            case WEDNESDAY -> "QUARTA";
            case THURSDAY -> "QUINTA";
            case FRIDAY -> "SEXTA";
            case SATURDAY -> "SABADO";
            case SUNDAY -> "DOMINGO";
        };
    }
}
