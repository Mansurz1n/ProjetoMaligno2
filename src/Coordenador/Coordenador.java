import java.util.*;

public class Coordenador
{
    static final int TAM_CALIBRACAO = 2_000_000; // bytes mandados a cada trabalhador na calibracao

    static volatile boolean falhou = false;

    interface Tarefa { void roda () throws Exception; }

    // embrulha a tarefa: se der erro, marca "falhou" em vez de sumir com a excecao
    static Runnable seguro (final Tarefa tarefa)
    {
        return new Runnable ()
        {
            public void run ()
            {
                try { tarefa.roda (); }
                catch (Exception erro)
                {
                    falhou = true;
                    System.err.println ("Erro: "+erro.getMessage());
                }
            }
        };
    }

    // dispara todas ao mesmo tempo e espera TODAS terminarem
    static void emParalelo (List<Runnable> tarefas) throws Exception
    {
        ArrayList<Thread> threads = new ArrayList<Thread> ();
        for (Runnable tarefa:tarefas)
        {
            Thread t = new Thread (tarefa);
            threads.add (t);
            t.start ();
        }
        for (Thread t:threads)
            t.join ();

        if (falhou)
            throw new Exception ("Um trabalhador falhou");
    }

    // ---------- 1) IP e porta de cada servidor ----------
    static ArrayList<InfoTrabalhador> lerTrabalhadores (String[] args, Scanner teclado)
            throws Exception
    {
        ArrayList<InfoTrabalhador> lista = new ArrayList<InfoTrabalhador> ();

        if (args.length>0) // java Coordenador ip:porta ip:porta ...
        {
            for (String a:args)
            {
                String[] hp = a.split (":");
                lista.add (new InfoTrabalhador (hp[0], Integer.parseInt(hp[1])));
            }
        }
        else               // ou pergunta no teclado
        {
            System.out.print ("Quantos trabalhadores? ");
            int quantos = Integer.parseInt (teclado.nextLine().trim());

            for (int i=1; i<=quantos; i++)
            {
                System.out.print ("IP do trabalhador "+i+": ");
                String ip = teclado.nextLine().trim();
                System.out.print ("Porta do trabalhador "+i+": ");
                int porta = Integer.parseInt (teclado.nextLine().trim());
                lista.add (new InfoTrabalhador(ip, porta));
            }
        }
        return lista;
    }

    // ---------- 2) Calibracao ----------
    // Todos recebem o MESMO vetor de teste; quem acabar primeiro e o melhor.
    static void calibrar (ArrayList<InfoTrabalhador> trabalhadores) throws Exception
    {
        final byte[] amostra = new byte [TAM_CALIBRACAO];
        new Random().nextBytes (amostra);

        System.out.println ("\nCalibrando com "+TAM_CALIBRACAO+" bytes por trabalhador...");

        ArrayList<Runnable> tarefas = new ArrayList<Runnable> ();
        for (final InfoTrabalhador t:trabalhadores)
            tarefas.add (seguro (new Tarefa ()
            {
                public void roda () throws Exception
                {
                    long inicio = System.nanoTime ();
                    t.ordenar (amostra);
                    t.esperarConcluido ();
                    t.setTempoCalibracao (System.nanoTime() - inicio);
                }
            }));
        emParalelo (tarefas);

        // melhor (menor tempo) primeiro
        Collections.sort (trabalhadores, new Comparator<InfoTrabalhador> ()
        {
            public int compare (InfoTrabalhador a, InfoTrabalhador b)
            {
                return Long.compare (a.getTempoCalibracao(), b.getTempoCalibracao());
            }
        });

        int pos = 1;
        for (InfoTrabalhador t:trabalhadores)
            System.out.printf ("  %do lugar: %s  (%.1f ms)%n",
                    pos++, t, t.getTempoCalibracao()/1_000_000.0);
    }

    static boolean estaOrdenado (byte[] v)
    {
        for (int i=1; i<v.length; i++)
            if (v[i-1] > v[i]) return false;
        return true;
    }

    public static void main (String[] args)
    {
        Scanner teclado = new Scanner (System.in);
        ArrayList<InfoTrabalhador> trabalhadores;

        try
        {
            trabalhadores = lerTrabalhadores (args, teclado);
            if (trabalhadores.isEmpty()) throw new Exception ();
        }
        catch (Exception erro)
        {
            System.err.println ("Uso: java Coordenador [ip:porta ...]  (ou rode sem argumentos)");
            return;
        }

        try
        {
            for (InfoTrabalhador t:trabalhadores)
                t.conectar ();

            // 2) calibra e ja deixa a lista ordenada do melhor pro pior
            calibrar (trabalhadores);

            System.out.print ("\nQuantos elementos tera o vetor? ");
            int n = Integer.parseInt (teclado.nextLine().trim());

            byte[] vetor = new byte [n];
            new Random().nextBytes (vetor);

            long inicioTotal = System.currentTimeMillis ();

            // ---------- divisao proporcional a velocidade ----------
            int qtd = trabalhadores.size ();
            double somaPesos = 0;
            for (InfoTrabalhador t:trabalhadores)
                somaPesos += 1.0 / t.getTempoCalibracao ();

            int[] inicio = new int [qtd+1];
            for (int i=0; i<qtd; i++)
            {
                int tam = (i==qtd-1) ? n - inicio[i]
                        : (int)(n * (1.0/trabalhadores.get(i).getTempoCalibracao()) / somaPesos);
                inicio[i+1] = inicio[i] + tam;
            }

            // ---------- FASE 1: cada um ordena o seu pedaco ----------
            ArrayList<Runnable> fase1 = new ArrayList<Runnable> ();
            for (int i=0; i<qtd; i++)
            {
                final InfoTrabalhador t = trabalhadores.get (i);
                final byte[] pedaco = Arrays.copyOfRange (vetor, inicio[i], inicio[i+1]);

                fase1.add (seguro (new Tarefa ()
                {
                    public void roda () throws Exception
                    {
                        System.out.println ("Enviando "+pedaco.length+" bytes para "+t);
                        t.ordenar (pedaco);
                        t.esperarConcluido ();   // so segue quando ELE avisar
                        System.out.println (t+" terminou de ordenar");
                    }
                }));
            }
            emParalelo (fase1);

            // ---------- FASE 2: rodadas de merge ----------
            // 'vivos' continua do melhor pro pior. A cada rodada:
            //   - a metade MELHOR vira receptora (faz o merge)
            //   - a metade PIOR doa o pedaco e sai
            //   - melhor receptor casa com o pior doador (o pior tem pedaco menor)
            ArrayList<InfoTrabalhador> vivos = new ArrayList<InfoTrabalhador> (trabalhadores);
            int rodada = 1;

            while (vivos.size() > 1)
            {
                final int metade = vivos.size() / 2;
                ArrayList<InfoTrabalhador> doadores =
                        new ArrayList<InfoTrabalhador> (vivos.subList (vivos.size()-metade, vivos.size()));

                System.out.println ("\nRodada "+rodada+": "+metade+" merge(s), "+vivos.size()+" maquinas");

                ArrayList<Runnable> tarefas = new ArrayList<Runnable> ();
                for (int i=0; i<metade; i++)
                {
                    final InfoTrabalhador receptor = vivos.get (i);
                    final InfoTrabalhador doador   = doadores.get (metade-1-i);

                    tarefas.add (seguro (new Tarefa ()
                    {
                        public void roda () throws Exception
                        {
                            // doador ja deu "Concluido" (fase 1 ou rodada anterior)
                            byte[] pedaco = doador.pegarResultado ();
                            doador.sair ();

                            System.out.println (receptor+" juntando com o pedaco de "+doador);
                            receptor.juntarCom (pedaco);
                            receptor.esperarConcluido ();
                        }
                    }));
                }
                emParalelo (tarefas);

                vivos.removeAll (doadores);
                rodada++;
            }

            // ---------- resultado final: o melhor trabalhador sobrou ----------
            InfoTrabalhador campeao = vivos.get (0);
            byte[] ordenado = campeao.pegarResultado ();
            campeao.sair ();

            long fimTotal = System.currentTimeMillis ();

            System.out.println ("\nTamanho final: "+ordenado.length);
            System.out.println ("Vetor esta ordenado? "+estaOrdenado (ordenado));
            System.out.println ("Tempo DISTRIBUIDO: "+(fimTotal-inicioTotal)+" ms");
        }
        catch (Exception erro)
        {
            System.err.println ("\nFalha: "+erro.getMessage());
            for (InfoTrabalhador t:trabalhadores) t.sair ();
        }
    }
}