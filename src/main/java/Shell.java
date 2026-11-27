import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Shell {

    private FileSystemSimulator simulador;
    private Scanner teclado = new Scanner(System.in);
    private boolean rodando = true;
    private String usuario = "simulador";
    private String maquina = "filesystem";

    public Shell(FileSystemSimulator simulador) {
        this.simulador = simulador;
    }

    public void iniciar() {
        System.out.println("simulador de sistema de arquivos - shell");
        System.out.println("digite start para ver os comandos disponíveis");

        while (rodando) {
            System.out.print(montarPrompt());

            if (!teclado.hasNextLine()) {
                break;
            }

            String entrada = teclado.nextLine().trim();
            if (entrada.isEmpty()) {
                continue;
            }

            String resultado = interpretar(entrada);
            if (!resultado.isEmpty()) {
                System.out.println(resultado);
            }
        }

        teclado.close();
    }

    private String montarPrompt() {
        String caminho = simulador.caminhoAtual();
        String local = "~";
        if (!caminho.equals("/")) {
            local = "~" + caminho;
        }
        return usuario + "@" + maquina + ":" + local + "$ ";
    }

    private String interpretar(String entrada) {
        String[] partes = entrada.split("\\s+");
        String comando = partes[0].toLowerCase();
        int quantidade = partes.length - 1;

        switch (comando) {
            case "mkdir":
                if (quantidade != 1) {
                    return "uso: mkdir nome";
                }
                return simulador.criarPasta(partes[1]);

            case "rmdir":
                if (quantidade != 1) {
                    return "uso: rmdir nome";
                }
                return simulador.apagarPasta(partes[1]);

            case "touch":
                if (quantidade != 1) {
                    return "uso: touch nome";
                }
                return simulador.criarArquivo(partes[1]);

            case "rm":
                if (quantidade != 1) {
                    return "uso: rm nome";
                }
                return simulador.apagarArquivo(partes[1]);

            case "mv":
                if (quantidade != 2) {
                    return "uso: mv antigo novo";
                }
                return simulador.renomear(partes[1], partes[2]);

            case "cp":
                if (quantidade != 2) {
                    return "uso: cp origem destino";
                }
                return simulador.copiarArquivo(partes[1], partes[2]);

            case "ls":
                if (quantidade == 0) {
                    return simulador.listar(null);
                }
                if (quantidade == 1) {
                    return simulador.listar(partes[1]);
                }
                return "uso: ls [pasta]";

            case "cd":
                if (quantidade != 1) {
                    return "uso: cd nome";
                }
                return simulador.entrarNaPasta(partes[1]);

            case "pwd":
                if (quantidade != 0) {
                    return "uso: pwd";
                }
                return simulador.caminhoAtual();

            case "clear":
                if (quantidade != 0) {
                    return "uso: clear";
                }
                limparTela();
                return "";

            case "start":
                if (quantidade != 0) {
                    return "uso: start";
                }
                return listaDeComandos();

            case "exit":
                if (quantidade != 0) {
                    return "uso: exit";
                }
                rodando = false;
                limparArquivos();
                return "encerrando o programa";

            case "falhar":
                
                if (quantidade == 2 && (partes[1].equals("1") || partes[1].equals("2"))) {
                    simulador.simularFalha(Integer.parseInt(partes[1]), partes[2]);
                    return "";
                }
                return "uso: falhar 1 nome  ou  falhar 2 nome";

            default:
                return "erro: comando inexistente: " + partes[0] + " (digite start para ver os comandos)";
        }
    }

    private void limparArquivos() {
        try {
            Files.write(Paths.get("dados", "filesystem.json"), new byte[0]);
            Files.write(Paths.get("dados", "journal.log"), new byte[0]);
        } catch (IOException e) {
            System.out.println("erro ao limpar os arquivos: " + e.getMessage());
        }
    }

    private void limparTela() {
        String sistema = System.getProperty("os.name").toLowerCase();
        try {
            if (sistema.contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    private String listaDeComandos() {
        return "comandos disponíveis:\n"
                + "  mkdir nome          cria uma pasta\n"
                + "  rmdir nome          apaga uma pasta vazia\n"
                + "  touch nome          cria um arquivo vazio\n"
                + "  rm nome             apaga um arquivo\n"
                + "  mv antigo novo      renomeia arquivo ou pasta\n"
                + "  cp origem destino   copia um arquivo\n"
                + "  ls [pasta]          lista arquivos e pastas\n"
                + "  cd nome             entra na pasta\n"
                + "  cd ..               volta para a pasta anterior\n"
                + "  cd /                volta para a raiz\n"
                + "  pwd                 mostra o diretório atual\n"
                + "  clear               limpa o terminal\n"
                + "  start               mostra esta lista\n"
                + "  exit                encerra o programa";
    }
}