import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class FileSystemSimulator {

    private Directory raiz;
    private String atual = "/";
    private Journal journal;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Path json = Paths.get("dados", "filesystem.json");

    public FileSystemSimulator() {
        journal = new Journal();
        carregar();
        recuperar();
    }

    public String caminhoAtual() {
        return atual;
    }


    public String criarPasta(String nome) {
        String caminho = resolver(nome);
        String erro = verificarNovo(caminho);
        if (erro != null) {
            return erro;
        }
        executar("MKDIR", caminho, "");
        return "pasta criada";
    }

    public String apagarPasta(String nome) {
        String caminho = resolver(nome);
        if (caminho.equals("/")) {
            return "erro: não é possível apagar a raiz";
        }
        Directory pasta = buscarDiretorio(caminho);
        if (pasta == null) {
            return "erro: a pasta não existe";
        }
        if (!pasta.estaVazia()) {
            return "erro: a pasta não está vazia";
        }
        if (atual.equals(caminho)) {
            return "erro: você está dentro dessa pasta";
        }
        executar("RMDIR", caminho, "");
        return "pasta apagada";
    }

    public String criarArquivo(String nome) {
        String caminho = resolver(nome);
        String erro = verificarNovo(caminho);
        if (erro != null) {
            return erro;
        }
        executar("TOUCH", caminho, "");
        return "arquivo criado";
    }

    public String apagarArquivo(String nome) {
        String caminho = resolver(nome);
        Directory pasta = buscarDiretorio(pai(caminho));
        if (pasta == null || pasta.buscarArquivo(nomeFinal(caminho)) == null) {
            return "erro: arquivo não encontrado";
        }
        executar("RM", caminho, "");
        return "arquivo apagado";
    }

    public String renomear(String antigo, String novo) {
        String caminho = resolver(antigo);
        if (caminho.equals("/")) {
            return "erro: não é possível renomear a raiz";
        }
        Directory pasta = buscarDiretorio(pai(caminho));
        String nome = nomeFinal(caminho);
        if (pasta == null || (pasta.buscarArquivo(nome) == null && pasta.buscarDiretorio(nome) == null)) {
            return "erro: arquivo ou pasta não encontrado";
        }
        if (!nomeValido(novo)) {
            return "erro: nome inválido";
        }
        if (existe(pasta, novo)) {
            return "erro: já existe um arquivo ou pasta com esse nome";
        }
        if (atual.equals(caminho) || atual.startsWith(caminho + "/")) {
            return "erro: você está dentro dessa pasta";
        }
        executar("RENOMEAR", caminho, novo);
        return "renomeado com sucesso";
    }

    public String copiarArquivo(String origem, String destino) {
        String caminho = resolver(origem);
        Directory pasta = buscarDiretorio(pai(caminho));
        String nome = nomeFinal(caminho);
        if (pasta == null || pasta.buscarArquivo(nome) == null) {
            return "erro: arquivo de origem não encontrado";
        }

        String completo = resolver(destino);
        if (buscarDiretorio(completo) != null) {
            completo = juntar(completo, nome);
        }

        String erro = verificarNovo(completo);
        if (erro != null) {
            return "erro: destino inválido (" + erro.replace("erro: ", "") + ")";
        }
        executar("COPIAR", caminho, completo);
        return "arquivo copiado";
    }

    public String listar(String nome) {
        String caminho = atual;
        if (nome != null) {
            caminho = resolver(nome);
        }
        Directory pasta = buscarDiretorio(caminho);
        if (pasta == null) {
            return "erro: a pasta não existe";
        }
        if (pasta.estaVazia()) {
            return "(vazia)";
        }

        StringBuilder texto = new StringBuilder();
        for (Directory diretorio : pasta.getDiretorios()) {
            texto.append("[pasta] ").append(diretorio.getNome()).append("\n");
        }
        for (File arquivo : pasta.getArquivos()) {
            texto.append(arquivo.getNome()).append("\n");
        }
        return texto.toString().trim();
    }

    public String entrarNaPasta(String nome) {
        String caminho = resolver(nome);
        if (buscarDiretorio(caminho) == null) {
            return "erro: a pasta não existe";
        }
        atual = caminho;
        return "";
    }

    
    public void simularFalha(int momento, String nome) {
        String caminho = resolver(nome);
        String erro = verificarNovo(caminho);
        if (erro != null) {
            System.out.println(erro);
            return;
        }
        journal.registrarInicio("MKDIR", caminho, "");
        if (momento == 2) {
            aplicar("MKDIR", caminho, "");
            salvar();
        }
        System.out.println("simulando queda do programa...");
        System.exit(1);
    }

    
    private void executar(String tipo, String caminho, String destino) {
        int numero = journal.registrarInicio(tipo, caminho, destino);
        aplicar(tipo, caminho, destino);
        salvar();
        journal.registrarConclusao(numero, tipo, caminho, destino);
    }

    private void recuperar() {
        List<String[]> pendentes = journal.buscarPendentes();
        for (String[] operacao : pendentes) {
            System.out.println("recuperando operação " + operacao[0] + ": " + operacao[2] + " " + operacao[3]);
            aplicar(operacao[2], operacao[3], operacao[4]);
            salvar();
            journal.registrarRecuperacao(Integer.parseInt(operacao[0]), operacao[2], operacao[3], operacao[4]);
        }
        if (!pendentes.isEmpty()) {
            System.out.println("recuperação concluída");
        }
    }

    
    private void aplicar(String tipo, String caminho, String destino) {
        Directory pasta = buscarDiretorio(pai(caminho));
        if (pasta == null) {
            return;
        }
        String nome = nomeFinal(caminho);

        switch (tipo) {
            case "MKDIR":
                if (!existe(pasta, nome)) {
                    pasta.getDiretorios().add(new Directory(nome));
                }
                break;
            case "RMDIR":
                pasta.getDiretorios().removeIf(diretorio -> diretorio.getNome().equals(nome));
                break;
            case "TOUCH":
                if (!existe(pasta, nome)) {
                    pasta.getArquivos().add(new File(nome, "", Journal.agora()));
                }
                break;
            case "RM":
                pasta.getArquivos().removeIf(arquivo -> arquivo.getNome().equals(nome));
                break;
            case "RENOMEAR":
                File arquivo = pasta.buscarArquivo(nome);
                Directory diretorio = pasta.buscarDiretorio(nome);
                if (arquivo != null) {
                    arquivo.setNome(destino);
                } else if (diretorio != null) {
                    diretorio.setNome(destino);
                }
                break;
            case "COPIAR":
                File original = pasta.buscarArquivo(nome);
                Directory pastadestino = buscarDiretorio(pai(destino));
                String novonome = nomeFinal(destino);
                if (original != null && pastadestino != null && !existe(pastadestino, novonome)) {
                    pastadestino.getArquivos().add(new File(novonome, original.getConteudo(), Journal.agora()));
                }
                break;
            default:
                break;
        }
    }


    private void carregar() {
        try {
            if (Files.exists(json)) {
                String texto = new String(Files.readAllBytes(json), StandardCharsets.UTF_8);
                raiz = gson.fromJson(texto, Directory.class);
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("erro ao carregar o sistema de arquivos: " + e.getMessage());
        }
        if (raiz == null) {
            raiz = new Directory("/");
        }
    }

    private void salvar() {
        try {
            Path temporario = Paths.get("dados", "filesystem.tmp");
            Files.write(temporario, gson.toJson(raiz).getBytes(StandardCharsets.UTF_8));
            Files.move(temporario, json, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("erro ao salvar: " + e.getMessage());
        }
    }

    
    private String resolver(String caminho) {
        String completo = caminho;
        if (!caminho.startsWith("/")) {
            completo = atual + "/" + caminho;
        }

        List<String> partes = new ArrayList<>();
        for (String parte : completo.split("/")) {
            if (parte.isEmpty() || parte.equals(".")) {
                continue;
            }
            if (parte.equals("..")) {
                if (!partes.isEmpty()) {
                    partes.remove(partes.size() - 1);
                }
            } else {
                partes.add(parte);
            }
        }
        return "/" + String.join("/", partes);
    }

    private Directory buscarDiretorio(String caminho) {
        Directory diretorio = raiz;
        for (String parte : caminho.split("/")) {
            if (parte.isEmpty()) {
                continue;
            }
            diretorio = diretorio.buscarDiretorio(parte);
            if (diretorio == null) {
                return null;
            }
        }
        return diretorio;
    }

    private String pai(String caminho) {
        int posicao = caminho.lastIndexOf("/");
        if (posicao <= 0) {
            return "/";
        }
        return caminho.substring(0, posicao);
    }

    private String nomeFinal(String caminho) {
        return caminho.substring(caminho.lastIndexOf("/") + 1);
    }

    private String juntar(String pasta, String nome) {
        if (pasta.equals("/")) {
            return "/" + nome;
        }
        return pasta + "/" + nome;
    }

    private boolean existe(Directory pasta, String nome) {
        return pasta.buscarArquivo(nome) != null || pasta.buscarDiretorio(nome) != null;
    }

    private boolean nomeValido(String nome) {
        return !nome.isEmpty() && !nome.contains("/") && !nome.contains(";")
                && !nome.equals(".") && !nome.equals("..");
    }

    private String verificarNovo(String caminho) {
        if (caminho.equals("/")) {
            return "erro: caminho inválido";
        }
        String nome = nomeFinal(caminho);
        if (!nomeValido(nome)) {
            return "erro: nome inválido";
        }
        Directory pasta = buscarDiretorio(pai(caminho));
        if (pasta == null) {
            return "erro: o caminho não existe";
        }
        if (existe(pasta, nome)) {
            return "erro: já existe um arquivo ou pasta com esse nome";
        }
        return null;
    }
}