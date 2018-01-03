package ca.junctionbox.experiments.collision;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Bipartisan creates (max) Main classes which increment the rev counter
 *  using either simple prefix increments or a synchronised block.
 *
 */
class Bipartisan implements Callable<Integer> {
    private final int max;
    private final boolean incOnly;

    public Bipartisan(final int max, final boolean incOnly) {
        this.max = max;
        this.incOnly = incOnly;
    }

    @Override
    public Integer call() throws Exception {
        int i = 0;
        Main m = null;

        for (; i < max; i++) {
            m = new Main(incOnly);
        }

        if (m != null) {
            // yolo retrieval
            System.out.printf("%X, %B\n", m.getRev(), incOnly);
        }
        return i;
    }
}

public class Main {
    private static volatile int rev = 0;

    public Main(final boolean incOnly) {
        if (incOnly) {
            ++rev;
        } else {
            synchronized (Main.class) {
               ++rev;
            }
        }
    }

    // this method doesn't synchronize rev.
    public synchronized int getRev() {
            return rev;
    }

    // nor does this method.
    private synchronized void incRev() {
        ++rev;
    }

    public static void main(final String[] args) {
        if (args.length != 1) {
            System.out.println("1 argument is required. 'inc' will be unsynchronised, anything else will be synchronised.");
            return;
        }

        // ensure more threads than the total number of processors is running.
        final int nThreads = (int) (Runtime.getRuntime().availableProcessors() * 1.25);
        final int max = (Integer.MAX_VALUE - 2) / nThreads;
        final boolean incOnly = args[0].equals("inc");
        final ExecutorService svc = Executors.newFixedThreadPool(nThreads);
        final ArrayList<Callable<Integer>> pots = new ArrayList<>(nThreads);

        System.out.printf("starting %d threads, each counting to %X.\n", nThreads, max);

        for (int i = 0; i < nThreads; i++) {
            pots.add(new Bipartisan(max, incOnly));
        }

        try {
            List<Future<Integer>> res = svc.invokeAll(pots);
            for (int i = 0; i < nThreads; i++) {
                System.out.printf("thread %d counted to %X and done = %B.\n", i, res.get(i).get(), res.get(i).isDone());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        svc.shutdown();

        if (rev != max * nThreads) {
            System.err.printf("got rev = %X, want %X.\n", rev, max * nThreads);
        } else {
            System.out.printf("yea got %X!\n", rev);
        }
    }
}
