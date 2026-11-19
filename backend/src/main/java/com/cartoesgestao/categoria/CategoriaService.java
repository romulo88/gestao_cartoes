package com.cartoesgestao.categoria;

import com.cartoesgestao.categoria.dto.CategoriaRequest;
import com.cartoesgestao.categoria.dto.CategoriaResponse;
import com.cartoesgestao.common.ConflictException;
import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.categoria.dto.SugestaoCategoriaResponse;
import com.cartoesgestao.compra.CompraRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    /** Categorias com que toda conta nova começa (nome, cor). O usuário edita à vontade. */
    private static final List<String[]> PADRAO = List.of(
            new String[] {"Alimentação", "#eb6834"},
            new String[] {"Mercado", "#008300"},
            new String[] {"Transporte", "#2a78d6"},
            new String[] {"Saúde", "#e34948"},
            new String[] {"Casa", "#eda100"},
            new String[] {"Assinaturas", "#6250d6"},
            new String[] {"Compras", "#e87ba4"},
            new String[] {"Lazer", "#1baf7a"},
            new String[] {"Educação", "#64748b"},
            new String[] {"Outros", "#94a3b8"});

    /** Quantas compras recentes a sugestão olha - de sobra para aprender o padrão. */
    private static final int HISTORICO_SUGESTAO = 1000;

    private final CategoriaRepository repository;
    private final CompraRepository compraRepository;

    public CategoriaService(CategoriaRepository repository, CompraRepository compraRepository) {
        this.repository = repository;
        this.compraRepository = compraRepository;
    }

    /** Categoria sugerida para uma descrição (histórico → palavras-chave → "Outros"). */
    @Transactional(readOnly = true)
    public SugestaoCategoriaResponse sugerir(Integer usuarioId, String descricao) {
        List<Categoria> categorias = repository.findByUsuarioIdOrderByNomeAsc(usuarioId);
        List<RegraSugestao.Anterior> historico = compraRepository
                .historicoDeCategorias(usuarioId, PageRequest.of(0, HISTORICO_SUGESTAO)).stream()
                .map(l -> new RegraSugestao.Anterior((String) l[0], (Integer) l[1]))
                .toList();
        RegraSugestao.Sugestao s = RegraSugestao.sugerir(
                descricao,
                historico,
                categorias.stream().map(c -> new RegraSugestao.CategoriaNome(c.getId(), c.getNome())).toList());
        if (s == null) return new SugestaoCategoriaResponse(null, null);
        CategoriaResponse cat = categorias.stream()
                .filter(c -> c.getId().equals(s.categoriaId()))
                .findFirst()
                .map(CategoriaResponse::de)
                .orElse(null);
        return new SugestaoCategoriaResponse(cat, cat == null ? null : s.origem().name().toLowerCase());
    }

    @Transactional
    public void criarPadrao(Integer usuarioId) {
        for (String[] p : PADRAO) {
            Categoria c = new Categoria();
            c.setUsuarioId(usuarioId);
            c.setNome(p[0]);
            c.setCor(p[1]);
            repository.save(c);
        }
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(Integer usuarioId) {
        return repository.findByUsuarioIdOrderByNomeAsc(usuarioId).stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public Categoria buscar(Integer usuarioId, Integer id) {
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
    }

    @Transactional
    public CategoriaResponse criar(Integer usuarioId, CategoriaRequest req) {
        if (repository.existsByUsuarioIdAndNomeIgnoreCase(usuarioId, req.nome().trim())) {
            throw new ConflictException("Você já tem uma categoria com esse nome");
        }
        Categoria c = new Categoria();
        c.setUsuarioId(usuarioId);
        aplicar(c, req);
        return CategoriaResponse.de(repository.save(c));
    }

    @Transactional
    public CategoriaResponse atualizar(Integer usuarioId, Integer id, CategoriaRequest req) {
        Categoria c = buscar(usuarioId, id);
        if (repository.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(usuarioId, req.nome().trim(), id)) {
            throw new ConflictException("Você já tem uma categoria com esse nome");
        }
        aplicar(c, req);
        return CategoriaResponse.de(c);
    }

    /** As compras da categoria ficam "sem categoria" (ON DELETE SET NULL no banco). */
    @Transactional
    public void excluir(Integer usuarioId, Integer id) {
        repository.delete(buscar(usuarioId, id));
    }

    private void aplicar(Categoria c, CategoriaRequest req) {
        c.setNome(req.nome().trim());
        c.setCor(req.cor() != null ? req.cor() : "#64748b");
        c.setLimiteMensal(req.limiteMensal());
    }
}
