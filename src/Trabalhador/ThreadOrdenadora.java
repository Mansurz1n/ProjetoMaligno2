package Trabalhador;

public class ThreadOrdenadora extends Thread {

    private byte[] vetor;
    private int inicio;
    private int fim;
    private int numero;

    public ThreadOrdenadora(byte[] vetor, int inicio, int fim, int numero) {
        this.vetor = vetor;
        this.inicio = inicio;
        this.fim = fim;
        this.numero = numero;
    }

    public void run() {
        try {
            Main.log("Thread ordenadora " + numero + " comecou (posicoes " + inicio + " a " + fim + ")");

            MergeSort.sort(vetor, inicio, fim);

            Main.log("Thread ordenadora " + numero + " terminou");
        } catch (Exception erro) {
            System.out.println("Erro na thread ordenadora " + numero + ": " + erro.getMessage());
        }
    }
}
