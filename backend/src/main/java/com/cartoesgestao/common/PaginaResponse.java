package com.cartoesgestao.common;

import java.util.List;

public record PaginaResponse<T>(List<T> itens, int pagina, int totalPaginas, long totalItens) {
}
