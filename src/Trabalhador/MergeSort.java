package Trabalhador;

public class MergeSort {

    /**
     * Ordena o vetor em ordem crescente utilizando o algoritmo Merge Sort.
     *
     * @param vetor vetor que será ordenado
     */
    public static void sort(byte[] vetor) {

        if (vetor == null || vetor.length < 2) {
            return;
        }

        mergeSort(vetor, 0, vetor.length - 1);
    }

    /**
     * Ordena so uma parte do vetor (de inicio ate fim),
     * usado pelas threads ordenadoras
     */
    public static void sort(byte[] vetor, int inicio, int fim) {
        mergeSort(vetor, inicio, fim);
    }

    /**
     * Divide o vetor recursivamente até chegar a subvetores
     * com apenas um elemento.
     */
    static void mergeSort(
            byte[] vetor,
            int inicio,
            int fim) {

        if (inicio >= fim) {
            return;
        }

        int meio = inicio + (fim - inicio) / 2;

        mergeSort(vetor, inicio, meio);
        mergeSort(vetor, meio + 1, fim);

        merge(vetor, inicio, meio, fim);
    }

    /**
     * Intercala duas partes já ordenadas do vetor.
     * (as threads juntadoras tambem chamam esse metodo)
     */
    public static void merge(
            byte[] vetor,
            int inicio,
            int meio,
            int fim) {

        int tamanho = fim - inicio + 1;
        byte[] auxiliar = new byte[tamanho];

        int esquerda = inicio;
        int direita = meio + 1;
        int posicao = 0;

        while (esquerda <= meio && direita <= fim) {

            if (vetor[esquerda] <= vetor[direita]) {
                auxiliar[posicao] = vetor[esquerda];
                esquerda++;
            } else {
                auxiliar[posicao] = vetor[direita];
                direita++;
            }

            posicao++;
        }

        while (esquerda <= meio) {
            auxiliar[posicao] = vetor[esquerda];
            esquerda++;
            posicao++;
        }

        while (direita <= fim) {
            auxiliar[posicao] = vetor[direita];
            direita++;
            posicao++;
        }

        for (int i = 0; i < tamanho; i++) {
            vetor[inicio + i] = auxiliar[i];
        }
    }
}
