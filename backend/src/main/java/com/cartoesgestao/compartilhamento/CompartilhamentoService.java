package com.cartoesgestao.compartilhamento;

import com.cartoesgestao.common.InvalidRequestException;
import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.compartilhamento.dto.CompartilhamentoDtos.CompartilhamentoResponse;
import com.cartoesgestao.compartilhamento.dto.CompartilhamentoDtos.CriadoResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompartilhamentoService {

    static final int MAX_POR_USUARIO = 20;

    private final CompartilhamentoRepository repository;

    public CompartilhamentoService(CompartilhamentoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CompartilhamentoResponse> listar(Integer usuarioId) {
        return repository.findByUsuarioIdOrderByCreatedAtDesc(usuarioId).stream()
                .map(CompartilhamentoResponse::de)
                .toList();
    }

    @Transactional
    public CriadoResponse criar(Integer usuarioId, String nome) {
        if (repository.countByUsuarioId(usuarioId) >= MAX_POR_USUARIO) {
            throw new InvalidRequestException("Limite de " + MAX_POR_USUARIO + " links. Revogue algum antes de criar outro.");
        }
        String token = TokenCompartilhamento.gerar();
        Compartilhamento c = new Compartilhamento();
        c.setUsuarioId(usuarioId);
        c.setNome(nome.trim());
        c.setTokenHash(TokenCompartilhamento.hash(token));
        repository.saveAndFlush(c);
        return new CriadoResponse(CompartilhamentoResponse.de(c), token);
    }

    /** Revogar = apagar: o link para de funcionar na hora. */
    @Transactional
    public void revogar(Integer usuarioId, Integer id) {
        repository.delete(repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Link não encontrado")));
    }

    /**
     * Dono do link (id do usuário). Token inválido, malformado ou revogado = 404, sem dizer
     * qual dos casos. Atualiza o "último acesso" no máximo a cada 5 minutos (cada abertura do
     * painel faz várias chamadas).
     */
    @Transactional
    public Integer resolver(String token) {
        if (!TokenCompartilhamento.formatoValido(token)) throw linkInvalido();
        Compartilhamento c = repository.findByTokenHash(TokenCompartilhamento.hash(token)).orElseThrow(this::linkInvalido);
        LocalDateTime agora = LocalDateTime.now();
        if (c.getUltimoAcesso() == null || c.getUltimoAcesso().isBefore(agora.minusMinutes(5))) {
            c.setUltimoAcesso(agora);
        }
        return c.getUsuarioId();
    }

    private ResourceNotFoundException linkInvalido() {
        return new ResourceNotFoundException("Link inválido ou revogado. Peça um novo a quem compartilhou.");
    }
}
