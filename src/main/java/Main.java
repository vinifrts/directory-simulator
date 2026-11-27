import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        boolean dentrojanela = args.length > 0 && args[0].equals("--terminal");
        String sistema = System.getProperty("os.name").toLowerCase();

        if (!dentrojanela && sistema.contains("windows")) {
            if (abrirJanela()) {
                return;
            }
            System.out.println("não foi possível abrir a janela, usando este terminal mesmo");
        }

        iniciarShell();
    }

    private static void iniciarShell() {
        FileSystemSimulator simulador = new FileSystemSimulator();

        Shell shell = new Shell(simulador);
        shell.iniciar();
    }

    private static boolean abrirJanela() {
        try {
            String java = Paths.get(System.getProperty("java.home"), "bin", "java.exe").toString();
            String classpath = System.getProperty("java.class.path");
            String pasta = Paths.get("").toAbsolutePath().toString();

            String comandos = "@echo off\r\n"
                    + "chcp 65001 > nul\r\n"
                    + "color 0A\r\n"
                    + "mode con: cols=110 lines=30\r\n"
                    + "title Simulador de Sistema de Arquivos\r\n"
                    + "cd /d \"" + pasta + "\"\r\n"
                    + "\"" + java + "\" -Dfile.encoding=UTF-8 -cp \"" + classpath + "\" Main --terminal\r\n"
                    + "if errorlevel 1 pause\r\n";

            Path bat = Files.createTempFile("simulador", ".bat");
            Files.write(bat, comandos.getBytes(StandardCharsets.UTF_8));

            new ProcessBuilder("cmd", "/c", "start", "Simulador de Arquivos", bat.toString()).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}   