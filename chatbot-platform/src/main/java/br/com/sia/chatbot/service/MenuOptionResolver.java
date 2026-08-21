package br.com.sia.chatbot.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class MenuOptionResolver {

    public String identify(String text) {
        String normalized = normalize(text);

        if (normalized.matches("[1-7]")) {
            return normalized;
        }

        if (containsAny(
                normalized,
                "menu",
                "inicio",
                "voltar"
        )) {
            return "menu";
        }

        if (containsAny(
                normalized,
                "indicacao",
                "indicacoes",
                "indicar",
                "joelho",
                "braco",
                "coluna",
                "rafael"
        )) {
            return "7";
        }

        if (containsAny(
                normalized,
                "guia unimed",
                "guia do plano",
                "guia de tratamento",
                "guia para tratamento",
                "codigo unimed",
                "codigo da unimed",
                "20103646",
                "biofeedback"
        )) {
            return "3";
        }

        if (containsAny(
                normalized,
                "falar com uma pessoa",
                "falar com alguem",
                "falar com atendente",
                "falar com a secretaria",
                "atendimento humano",
                "secretaria",
                "atendente",
                "humano"
        )) {
            return "6";
        }

        if (containsAny(
                normalized,
                "quanto custa",
                "qual o valor",
                "quais os valores",
                "valor",
                "valores",
                "preco",
                "precos",
                "pagamento",
                "consulta",
                "sessao"
        )) {
            return "6";
        }

        if (containsAny(
                normalized,
                "que horas",
                "horario",
                "horarios",
                "funcionamento",
                "abre",
                "fecha"
        )) {
            return "4";
        }

        if (containsAny(
                normalized,
                "onde fica",
                "como chegar",
                "endereco",
                "localizacao",
                "estacionamento",
                "manobrista",
                "barata ribeiro"
        )) {
            return "5";
        }

        /*
         * O público atendido deixou de ter opção própria no menu.
         * O conteúdo de TREATMENTS já descreve mulheres, homens e
         * crianças, portanto absorve essas intenções.
         */
        if (containsAny(
                normalized,
                "tratamento",
                "tratamentos",
                "fisioterapia",
                "servico",
                "servicos",
                "quem atende",
                "mulher",
                "mulheres",
                "homem",
                "homens",
                "crianca",
                "criancas"
        )) {
            return "1";
        }

        if (containsAny(
                normalized,
                "plano",
                "planos",
                "convenio",
                "convenios",
                "unimed",
                "amil",
                "hapvida",
                "notre dame",
                "petrobras",
                "medsenior",
                "care plus"
        )) {
            return "2";
        }

        return "unknown";
    }

    private boolean containsAny(
            String text,
            String... terms
    ) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}