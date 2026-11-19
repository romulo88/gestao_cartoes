package com.cartoesgestao.auth;

import com.cartoesgestao.auth.dto.CadastroRequest;
import com.cartoesgestao.auth.dto.LoginRequest;
import com.cartoesgestao.auth.dto.SessaoResponse;
import com.cartoesgestao.auth.dto.TrocarSenhaRequest;
import com.cartoesgestao.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public SessaoResponse cadastrar(@Valid @RequestBody CadastroRequest req) {
        return authService.cadastrar(req);
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    /** Troca um token ainda válido por um novo (o frontend chama enquanto o usuário usa a tela). */
    @PostMapping("/renovar")
    public SessaoResponse renovar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return authService.renovar(usuario.id());
    }

    @PostMapping("/trocar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(
            @AuthenticationPrincipal UsuarioAutenticado usuario, @Valid @RequestBody TrocarSenhaRequest req) {
        authService.trocarSenha(usuario.id(), req);
    }
}
