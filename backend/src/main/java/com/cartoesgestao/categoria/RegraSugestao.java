package com.cartoesgestao.categoria;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sugestão de categoria a partir da descrição da compra, sem dependência de Spring/JPA
 * (testável isolado). Ordem de tentativa, decidida com o Romulo:
 * <ol>
 *   <li><b>Histórico do próprio usuário</b> - compras com descrição igual (normalizada) ou,
 *       não havendo, com a mesma primeira palavra; vence a categoria mais usada (empate: a
 *       mais recente). Aprende sozinho: corrigiu uma vez, a próxima vem certa.</li>
 *   <li><b>Palavras-chave padrão</b> ({@link #PALAVRAS_CHAVE}), casadas com as categorias do
 *       usuário pelo NOME (normalizado) - categoria renomeada deixa de receber as palavras.</li>
 *   <li><b>"Outros"</b>, se existir; senão nenhuma.</li>
 * </ol>
 * Sem tela de edição de palavras-chave (decisão do Romulo): o histórico cobre os casos
 * particulares de cada um.
 */
public final class RegraSugestao {

    private RegraSugestao() {
    }

    public enum Origem { HISTORICO, PALAVRA_CHAVE, PADRAO }

    public record Sugestao(Integer categoriaId, Origem origem) {
    }

    /** Uma compra anterior: descrição e categoria. A lista vem da mais recente para a mais antiga. */
    public record Anterior(String descricao, Integer categoriaId) {
    }

    /** Categoria do usuário (id + nome), para casar com as palavras-chave e achar "Outros". */
    public record CategoriaNome(Integer id, String nome) {
    }

    /** Prefixos que as faturas colocam antes do nome do estabelecimento ("MP *LOJA",
     * "PAG*FULANO", "EC *..."): não identificam nada, então saem antes de comparar. */
    private static final Set<String> PREFIXOS_IGNORADOS = Set.of(
            "mp", "mercadopago", "pag", "pg", "pagseguro", "ec", "dl", "pp", "paypal", "sumup", "stone", "cielo");

    /** Chave = nome da categoria padrão, normalizado (ordem de prioridade - ver o bloco static). Palavra de um termo casa com uma
     * palavra da descrição (ou o começo dela, se tiver 4+ letras: "uber" casa "ubertrip");
     * termo com espaço casa como trecho. */
    static final Map<String, List<String>> PALAVRAS_CHAVE = new LinkedHashMap<>();

    static {
        // A ORDEM importa: a primeira categoria que casar vence. Por isso "amazon prime"
        // (Assinaturas) vem antes de "amazon" (Compras), "mercado livre" (Compras) antes de
        // "mercado" (Mercado) e "uber eats" (Alimentação) antes de "uber" (Transporte).
        PALAVRAS_CHAVE.put("assinaturas", List.of(
                "netflix", "spotify", "deezer", "prime video", "amazon prime", "disney", "globoplay", "hbo", "max",
                "youtube", "icloud", "apple com", "google one", "microsoft", "adobe", "chatgpt", "openai", "claude",
                "anthropic", "paramount", "crunchyroll", "dropbox"));
        PALAVRAS_CHAVE.put("educacao", List.of(
                "escola", "colegio", "faculdade", "universidade", "curso", "udemy", "alura", "coursera", "livraria",
                "saraiva", "amazon kindle", "kindle", "papelaria", "material escolar", "mensalidade escolar"));
        PALAVRAS_CHAVE.put("compras", List.of(
                "amazon", "mercado livre", "mercadolivre", "shopee", "shein", "aliexpress", "magalu", "magazine luiza",
                "americanas", "casas bahia", "renner", "riachuelo", "zara", "centauro", "netshoes", "kabum",
                "decathlon", "havan", "boticario", "natura", "sephora", "temu"));
        PALAVRAS_CHAVE.put("alimentacao", List.of(
                "ifood", "ifd", "rappi", "uber eats", "ubereats", "restaurante", "lanchonete", "padaria", "burger", "mcdonalds", "bk", "pizza",
                "pizzaria", "churrascaria", "bar", "cafe", "cafeteria", "starbucks", "subway", "habibs", "sushi",
                "acai", "sorveteria", "lanches", "outback", "giraffas", "spoleto"));
        PALAVRAS_CHAVE.put("transporte", List.of(
                "uber", "99", "99app", "99pop", "cabify", "indrive", "posto", "shell", "ipiranga", "petrobras", "br mania",
                "combustivel", "estacionamento", "estapar", "pedagio", "sem parar", "conectcar", "veloe", "metro",
                "onibus", "passagem", "latam", "gol linhas", "azul linhas"));
        PALAVRAS_CHAVE.put("mercado", List.of(
                "mercado", "supermercado", "atacadao", "assai", "carrefour", "extra", "pao de acucar", "gbarbosa",
                "atakarejo", "mateus", "sams club", "hortifruti", "sacolao", "dia supermercado", "big bompreco",
                "bompreco", "makro", "comper", "zaffari", "sonda"));
        PALAVRAS_CHAVE.put("saude", List.of(
                "farmacia", "drogasil", "droga raia", "drogaria", "pague menos", "panvel", "drogao", "unimed",
                "hapvida", "amil", "bradesco saude", "sulamerica", "laboratorio", "clinica", "hospital", "consulta",
                "odonto", "dentista", "otica", "smartfit", "smart fit", "academia", "bluefit"));
        PALAVRAS_CHAVE.put("casa", List.of(
                "energia", "energisa", "cemig", "enel", "neoenergia", "coelba", "light", "saneamento", "deso",
                "copasa", "sabesp", "embasa", "agua", "condominio", "aluguel", "leroy", "telhanorte",
                "tok stok", "tokstok", "camicado", "vivo", "claro", "tim", "oi fibra", "internet", "gas"));
        PALAVRAS_CHAVE.put("lazer", List.of(
                "cinema", "cinemark", "kinoplex", "ingresso", "ingressocom", "sympla", "eventim", "steam",
                "playstation", "psn", "xbox", "nintendo", "hotel", "pousada", "airbnb", "booking", "decolar",
                "show", "teatro", "parque"));
    }

    public static Sugestao sugerir(String descricao, List<Anterior> historico, List<CategoriaNome> categorias) {
        String alvo = normalizar(descricao);
        if (alvo.isEmpty()) return null;

        // 1. Histórico: igualdade da descrição normalizada; senão, mesma primeira palavra.
        Integer porHistorico = maisUsada(alvo, historico, true);
        if (porHistorico == null) {
            porHistorico = maisUsada(alvo, historico, false);
        }
        if (porHistorico != null) return new Sugestao(porHistorico, Origem.HISTORICO);

        // 2. Palavras-chave: primeira categoria (na ordem do mapa) cuja palavra aparece.
        Map<String, Integer> idPorNome = new HashMap<>();
        for (CategoriaNome c : categorias) idPorNome.put(normalizar(c.nome()), c.id());
        for (Map.Entry<String, List<String>> e : PALAVRAS_CHAVE.entrySet()) {
            Integer id = idPorNome.get(e.getKey());
            if (id != null && contemAlguma(alvo, e.getValue())) return new Sugestao(id, Origem.PALAVRA_CHAVE);
        }

        // 3. "Outros".
        Integer outros = idPorNome.get("outros");
        return outros == null ? null : new Sugestao(outros, Origem.PADRAO);
    }

    /** Minúsculas, sem acento, só letras/números, sem prefixos de maquininha e sem números
     * longos (o "4417" de "UBER *TRIP 4417"). Números curtos ficam ("99"). */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String s = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]+", " ").trim();
        List<String> tokens = new ArrayList<>(Arrays.asList(s.split(" ")));
        tokens.removeIf(t -> t.isEmpty() || t.matches("\\d{3,}"));
        while (tokens.size() > 1 && PREFIXOS_IGNORADOS.contains(tokens.get(0))) tokens.remove(0);
        return String.join(" ", tokens);
    }

    private static Integer maisUsada(String alvo, List<Anterior> historico, boolean exata) {
        String primeira = alvo.split(" ")[0];
        // Primeira palavra curta ("de", "a") não identifica nada.
        if (!exata && primeira.length() < 3) return null;
        Map<Integer, Integer> contagem = new LinkedHashMap<>(); // ordem de inserção = mais recente primeiro
        for (Anterior a : historico) {
            if (a.categoriaId() == null) continue;
            String d = normalizar(a.descricao());
            boolean casa = exata ? d.equals(alvo) : d.split(" ")[0].equals(primeira);
            if (casa) contagem.merge(a.categoriaId(), 1, Integer::sum);
        }
        Integer melhor = null;
        int max = 0;
        for (Map.Entry<Integer, Integer> e : contagem.entrySet()) {
            if (e.getValue() > max) { // ">" e não ">=": no empate fica a mais recente
                melhor = e.getKey();
                max = e.getValue();
            }
        }
        return melhor;
    }

    private static boolean contemAlguma(String alvo, List<String> palavras) {
        String comEspacos = " " + alvo + " ";
        List<String> tokens = Arrays.asList(alvo.split(" "));
        for (String p : palavras) {
            if (p.contains(" ")) {
                if (comEspacos.contains(" " + p + " ")) return true;
            } else {
                for (String t : tokens) {
                    if (t.equals(p) || (p.length() >= 4 && t.startsWith(p))) return true;
                }
            }
        }
        return false;
    }
}
