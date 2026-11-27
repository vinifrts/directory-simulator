import java.util.ArrayList;
import java.util.List;

public class Directory {

    private String nome;
    private List<File> arquivos = new ArrayList<>();
    private List<Directory> diretorios = new ArrayList<>();

    public Directory(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public List<File> getArquivos() {
        return arquivos;
    }

    public List<Directory> getDiretorios() {
        return diretorios;
    }

    public File buscarArquivo(String nome) {
        for (File arquivo : arquivos) {
            if (arquivo.getNome().equals(nome)) {
                return arquivo;
            }
        }
        return null;
    }

    public Directory buscarDiretorio(String nome) {
        for (Directory diretorio : diretorios) {
            if (diretorio.getNome().equals(nome)) {
                return diretorio;
            }
        }
        return null;
    }

    public boolean estaVazia() {
        return arquivos.isEmpty() && diretorios.isEmpty();
    }
}