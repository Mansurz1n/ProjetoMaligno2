package Coordenador;

import Trabalhador.Teclado;
import Trabalhador.ThreadJuntadora;
import Trabalhador.ThreadOrdenadora;

import java.util.Random;

// PROGRAMA PARALELO (o sequencial esta na classe Sequencial)
public class Main {

    static void log(String mensagem) {
        System.out.println("[LOG " + java.time.LocalTime.now() + "] " + mensagem);
    }

    static void mostrarQtdProcessadores() {
        int quantidade = Runtime.getRuntime().availableProcessors();
        System.out.println("Quantidade de processadores: " + quantidade);
    }

    static int pedirTamanho() {
        int tamanho = 0;

        while (tamanho <= 0) {
            System.out.print("Quantos elementos vai ter o vetor? ");
            try {
                tamanho = Trabalhador.Teclado.getUmInt();
                if (tamanho <= 0) {
                    System.out.println("Tem que ser maior que zero!");
                }
            } catch (Exception erro) {
                System.out.println("Numero invalido, tente de novo");
            }
        }

        return tamanho;
    }

    static byte[] escolhaPreenchimento(int tamanhoVetor) throws Exception {
        int escolha = 0;
        Random random = new Random();

        byte[] vetor = new byte[tamanhoVetor];

        while (escolha != 1 && escolha != 2) {
            System.out.println("1 - Preencher a mao");
            System.out.println("2 - Preencher automatico (random)");
            try {
                escolha = Trabalhador.Teclado.getUmInt();
            } catch (Exception erro) {
                System.out.println("Opcao invalida");
            }
        }

        if (escolha == 1) {
            for (int i = 0; i < tamanhoVetor; i++) {
                boolean ok = false;
                while (!ok) {
                    System.out.print("vetor[" + i + "] = ");
                    try {
                        vetor[i] = Trabalhador.Teclado.getUmByte();
                        ok = true;
                    } catch (Exception erro) {
                        System.out.println("Byte invalido (de -128 ate 127)");
                    }
                }
            }
        } else {
            random.nextBytes(vetor);
        }

        return vetor;
    }

    static void visualizacaoLista(byte[] vetor) {
        int escolha = 0;

        while (escolha < 1 || escolha > 3) {
            System.out.println("O que quer printar do vetor ordenado?");
            System.out.println("1 - Tudo");
            System.out.println("2 - So uma parte");
            System.out.println("3 - Nada");
            try {
                escolha = Trabalhador.Teclado.getUmInt();
            } catch (Exception erro) {
                System.out.println("Opcao invalida");
            }
        }

        int inicio = 0;
        int fim = vetor.length - 1;

        if (escolha == 3) {
            return;
        }

        if (escolha == 2) {
            inicio = -1;
            while (inicio < 0 || inicio > vetor.length - 1) {
                System.out.print("Posicao inicial (0 ate " + (vetor.length - 1) + "): ");
                try {
                    inicio = Trabalhador.Teclado.getUmInt();
                } catch (Exception erro) {
                    System.out.println("Numero invalido");
                }
            }

            fim = -1;
            while (fim < inicio || fim > vetor.length - 1) {
                System.out.print("Posicao final (" + inicio + " ate " + (vetor.length - 1) + "): ");
                try {
                    fim = Teclado.getUmInt();
                } catch (Exception erro) {
                    System.out.println("Numero invalido");
                }
            }
        }

        for (int i = inicio; i <= fim; i++) {
            System.out.print(vetor[i] + " ");
        }
        System.out.println();
    }

    static boolean estaOrdenado(byte[] vetor) {
        for (int i = 1; i < vetor.length; i++) {
            if (vetor[i - 1] > vetor[i]) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws Exception {

        log("Programa PARALELO iniciado");
        mostrarQtdProcessadores();

        int tamanho = pedirTamanho();
        byte[] vetor = escolhaPreenchimento(tamanho);
        log("Vetor preenchido com " + tamanho + " elementos");

        // threads ordenadoras = processadores - 1
        int qtdThreads = Runtime.getRuntime().availableProcessors() - 1;
        if (qtdThreads < 1) {
            qtdThreads = 1; // se so tiver 1 processador
        }
        if (qtdThreads > tamanho) {
            qtdThreads = tamanho;
        }
        log("Vamos usar " + qtdThreads + " threads ordenadoras");

        long inicioTempo = System.currentTimeMillis();

        // limites de cada pedaco: pedaco i vai de limites[i] ate limites[i+1]-1
        int[] limites = new int[qtdThreads + 1];
        for (int i = 0; i <= qtdThreads; i++) {
            limites[i] = (int) ((long) tamanho * i / qtdThreads);
        }

        // ---- threads ordenadoras ----
        ThreadOrdenadora[] ordenadoras = new ThreadOrdenadora[qtdThreads];

        for (int i = 0; i < qtdThreads; i++) {
            ordenadoras[i] = new ThreadOrdenadora(vetor, limites[i], limites[i + 1] - 1, i + 1);
            ordenadoras[i].start();
        }

        for (int i = 0; i < qtdThreads; i++) {
            try {
                ordenadoras[i].join();
            } catch (InterruptedException erro) {
                System.out.println("Thread ordenadora " + (i + 1) + " foi interrompida");
            }
        }
        log("Todas as ordenadoras terminaram");

        // ---- threads juntadoras (varias rodadas) ----
        int rodada = 1;

        while (limites.length - 1 > 1) {
            int qtdPedacos = limites.length - 1;
            int qtdJuntadoras = qtdPedacos / 2;

            log("Rodada " + rodada + ": " + qtdJuntadoras + " juntadora(s) para " + qtdPedacos + " pedacos");

            int[] novosLimites;
            if (qtdPedacos % 2 == 0) {
                novosLimites = new int[qtdJuntadoras + 1];
            } else {
                novosLimites = new int[qtdJuntadoras + 2]; // sobra um pedaco
            }

            ThreadJuntadora[] juntadoras = new ThreadJuntadora[qtdJuntadoras];

            for (int i = 0; i < qtdJuntadoras; i++) {
                int ini = limites[2 * i];
                int meio = limites[2 * i + 1] - 1;
                int fim = limites[2 * i + 2] - 1;

                juntadoras[i] = new ThreadJuntadora(vetor, ini, meio, fim, i + 1);
                juntadoras[i].start();

                novosLimites[i + 1] = limites[2 * i + 2];
            }

            if (qtdPedacos % 2 == 1) {
                novosLimites[novosLimites.length - 1] = limites[qtdPedacos];
            }

            for (int i = 0; i < qtdJuntadoras; i++) {
                try {
                    juntadoras[i].join();
                } catch (InterruptedException erro) {
                    System.out.println("Thread juntadora " + (i + 1) + " foi interrompida");
                }
            }

            limites = novosLimites;
            rodada++;
        }

        long fimTempo = System.currentTimeMillis();
        log("Ordenacao terminou");

        System.out.println("Vetor esta ordenado? " + estaOrdenado(vetor));
        System.out.println("Tempo PARALELO: " + (fimTempo - inicioTempo) + " ms");

        visualizacaoLista(vetor);
    }
}
