package com.cartoesgestao.auth;

import com.cartoesgestao.auth.dto.CadastroRequest;
import com.cartoesgestao.auth.dto.LoginRequest;
import com.cartoesgestao.auth.dto.SessaoResponse;
import com.cartoesgestao.auth.dto.TrocarSenhaRequest;
import com.cartoesgestao.categoria.CategoriaService;
import com.cartoesgestao.common.ConflictException;
import com.cartoesgestao.common.InvalidRequestException;
import com.cartoesgestao.common.ResourceNotFoundException;
import com.cartoesgestao.common.UnauthorizedException;
import com.cartoesgestao.security.JwtService;
import com.cartoesgestao.usuario.Usuario;
import com.cartoesgestao.usuario.UsuarioRepository;
import java.time.LocalDateTime;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoriaService categoriaService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CategoriaService categoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.categoriaService = categoriaService;
    }

    /** Autocadastro aberto (é um SaaS): cria a conta já com as categorias padrão e devolve
     * a sessão, sem precisar logar de novo. */
    @Transactional
    public SessaoResponse cadastrar(CadastroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Já existe uma conta com esse e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(req.senha()));
        usuario.setUltimoLogin(LocalDateTime.now());
        usuarioRepository.save(usuario);
        categoriaService.criarPadrao(usuario.getId());
        return sessao(usuario);
    }

    @Transactional
    public SessaoResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmail(req.email().trim())
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                // Mesma mensagem para e-mail inexistente e senha errada: não revela quem tem conta.
                .orElseThrow(() -> new UnauthorizedException("E-mail ou senha inválidos"));
        usuario.setUltimoLogin(LocalDateTime.now());
        return sessao(usuario);
    }

    @Transactional(readOnly = true)
    public SessaoResponse renovar(Integer usuarioId) {
        return sessao(buscar(usuarioId));
    }

    @Transactional
    public void trocarSenha(Integer usuarioId, TrocarSenhaRequest req) {
        Usuario usuario = buscar(usuarioId);
        if (!passwordEncoder.matches(req.senhaAtual(), usuario.getSenhaHash())) {
            throw new InvalidRequestException("Senha atual incorreta");
        }
        usuario.setSenhaHash(passwordEncoder.encode(req.novaSenha()));
    }

    private Usuario buscar(Integer usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private SessaoResponse sessao(Usuario u) {
        return new SessaoResponse(jwtService.gerarToken(u), u.getId(), u.getNome(), u.getEmail());
    }
}
