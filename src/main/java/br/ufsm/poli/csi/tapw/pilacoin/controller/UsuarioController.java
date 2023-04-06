package br.ufsm.poli.csi.tapw.pilacoin.controller;

import br.ufsm.poli.csi.tapw.pilacoin.model.PilaCoin;
import br.ufsm.poli.csi.tapw.pilacoin.model.Usuario;
import br.ufsm.poli.csi.tapw.pilacoin.repository.UsuarioRepository;
import br.ufsm.poli.csi.tapw.pilacoin.service.PilaCoinService;
import br.ufsm.poli.csi.tapw.pilacoin.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Controller
public class UsuarioController {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    PilaCoinService pilaCoinService;

    @GetMapping("/")
    public String loginPage() {
        return "index";
    }

    @PostMapping("/login")
    public String login(Model model, @RequestParam String nome, @RequestParam MultipartFile arqChavePrivada, HttpServletRequest request) throws IOException {
        Usuario usuarioDB = usuarioRepository.findByNome(nome);
        byte[] bArrPrivateKey = arqChavePrivada.getBytes();
        boolean keyPairVerifierResult;
        try {
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(usuarioDB.getChavePublica()));
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bArrPrivateKey));
            keyPairVerifierResult = Utils.keyPairVerifier(publicKey, privateKey);
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Nome ou chave privada ínválido!");
            return "index";
        }

        if (keyPairVerifierResult) {
            HttpSession session = request.getSession();
            session.setAttribute("nome", usuarioDB.getNome());
            session.setAttribute("chavePublica", usuarioDB.getChavePublica());
            return "redirect:/home";
        }

        model.addAttribute("errorMessage", "Nome ou chave privada ínválido!");
        return "index";
    }

    @GetMapping("/home")
    public String home(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        if (session.getAttribute("nome") != null) {
            List<PilaCoin> pilas = pilaCoinService.buscaPilacoinUsuario(Utils.getKeyBytes("public_key.der"));
            model.addAttribute("pilas", pilas);
            model.addAttribute("total", pilas.size());
            return "home";
        }
        else
            return "redirect:/";
    }
}
