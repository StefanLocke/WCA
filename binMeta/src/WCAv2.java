import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.lang.*;

public class WCAv2 extends binMeta {
	private int MAX_TRIES = 2000;
	
	
	/**
	 * 
	 * @param startPoint
	 * @param Obj
	 * @param might lock itself if set too long.
	 */
	public WCAv2(Data startPoint, Objective Obj, long maxTime) {
		idcount = 0;
		this.metaName = "Water cycle Algorithm";
		this.solution = startPoint;
		this.obj = Obj;
		this.maxTime = maxTime;
		this.objValue = this.obj.value(this.solution);
		workers = new LinkedList<>();
		;
		threadsRan = 0;
		generate(10, 20, 0.0);
		designateAll();
		assign();
	}
	Objective obj;
	int threadsRan; // a counter for the amount of child treads completed
	private int idcount;  // a counter for assigning ids
	private int Nsr;  //number of seas and rivers
	private int Npop; //total number of streams
	List<Stream> pop; //list of all the streams
	List<Thread> workers; //list of all the workers
	Semaphore S = new Semaphore(1); //semaphore for accessing obj (because of the way certain obj are coded)

	public int newId() { // id assigner
		return idcount++;
	}

	public void generate(int numberOfSeaAndRivers, int numberOfTotal, double dMax) { //generates a ranom sample of streams and sorts them to rivers or seas
		this.Nsr = numberOfSeaAndRivers;
		this.Npop = numberOfTotal;
		pop = new LinkedList<Stream>();
		for (int i = 0; i < numberOfTotal; i++) {
			pop.add(new Stream(obj.solutionSample()));
		}
		pop.sort(null);

		for (int i = 0; i < numberOfSeaAndRivers; i++) {
			if (i == 0) {
				pop.get(i).type = streamType.Sea;
			} else {
				pop.get(i).type = streamType.River;
			}
		}

	}
	
	
	//--------------------------------------------------------------------------------
	// DESIGNATIONS OF NUMBER OF STREAMS PER EACH RIVERS
	// -------------------------------------------------------------------------------	
		

	private int NstreamsDesignated;//number of designated streams if == number of streams, it means all streams have been designated

	public void designateAll() {
		NstreamsDesignated = 0;
		for (Stream s : pop) {
			if (s.type == streamType.River || s.type == streamType.Sea) {
				int i = designateStreams(s);
				s.nAssignedStreams = i;
				NstreamsDesignated = NstreamsDesignated + i;
			}
		}

		while (NstreamsDesignated < Npop - Nsr) {
			Random r = new Random();
			Stream rv = pop.get(r.nextInt(Nsr));
			rv.nAssignedStreams++;
			NstreamsDesignated++;
		}

		while (NstreamsDesignated > Npop - Nsr) {
			Random r = new Random();
			Stream rv = pop.get(r.nextInt(Nsr));
			if (rv.nAssignedStreams > 0) {
				rv.nAssignedStreams--;
				NstreamsDesignated--;
			}
		}
	}

	private int designateStreams(Stream rs) {  //designates a number of streams for a given river or sea
		double Cn = obj.value(rs.data) - obj.value(pop.get(Nsr).data);
		double sum = 0;
		for (int i = 0; i < Nsr; i++) {
			sum = sum + (obj.value(pop.get(i).data) - obj.value(pop.get(Nsr).data));
		}
		double bfore = Math.abs(Cn / sum) * (Npop - Nsr);
		long NSn = Math.round(bfore);
		return (int) NSn;
	}

	//--------------------------------------------------------------------------------
	// ASSIGNMENTS OF STREAMS TO RIVERS
	// -------------------------------------------------------------------------------	
	
	
	private void assignRandomUnassignedTo(Stream r) {
		List<Stream> onlyStreams = new LinkedList<>(pop);

		Iterator<Stream> it = onlyStreams.iterator();
		while (it.hasNext()) {
			Stream s = it.next();
			if (s.type == streamType.River || s.type == streamType.Sea) {
				it.remove();
			} else {
				if (s.assigned == true) {
					it.remove();
				}
			}
		}

		Random rand = new Random();

		while (r.children.size() < r.nAssignedStreams) {
			int i = rand.nextInt(onlyStreams.size());
			onlyStreams.get(i).assigned = true;
			r.children.add(onlyStreams.remove(i));
		}

	}

	public void assign() {
		for (Stream s : pop) {
			if (s.type == streamType.River || s.type == streamType.Sea) {
				assignRandomUnassignedTo(s);
			}
		}
	}

	public Data getDataOfValue(double value, double threshold) {
		Data d = obj.solutionSample();
		while (Math.abs(obj.value(d) - value) < threshold) {
			d = obj.solutionSample();
			threshold = threshold * 0.01;
		}
		return d;
	}

//---------------------------------------------------------------------------------------------		
// MAIN LOOP // -------------------------------------------------------------------------------	
//---------------------------------------------------------------------------------------------		

	
	//---------------------------------------------------------------------------------------------		
	// moving the streams // -------------------------------------------------------------------------------	
	//---------------------------------------------------------------------------------------------			
	
	
	public void moveStreams() {
		for (Stream s : pop) {
			if (s.type == streamType.River || s.type == streamType.Sea) {
				Thread w = new StreamWorker(s);
				workers.add(w);
				w.start();
			}
		}

		for (Thread t : workers) {
			try {
				t.join();
				threadsRan++;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		workers.clear();
	}
	
	//---------------------------------------------------------------------------------------------		
	// swapping the streams // -------------------------------------------------------------------------------	
	//---------------------------------------------------------------------------------------------			

	public void swapStreams(Stream river) {
		Data tmpdata = river.children.get(0).data;
		river.children.getFirst().data = river.data;
		river.data = tmpdata;
	}

	public void swapStreams() {
		for (Stream s : pop) {
			if (s.type == streamType.River || s.type == streamType.Sea) {
				s.children.sort(null);
				if (!s.children.isEmpty()) {
					if (obj.value(s.data) > obj.value(s.children.get(0).data)) {
						swapStreams(s);
					}
				}
			}
		}
	}
	//---------------------------------------------------------------------------------------------		
	// moving the rivers // -------------------------------------------------------------------------------	
	//---------------------------------------------------------------------------------------------			

	private void moveRivers() {
		for (Stream s : pop) {
			if (s.type == streamType.River) {
				Thread w = new RiverWorker(s);
				workers.add(w);
				w.start();
			}
		}

		for (Thread t : workers) {
			try {
				t.join();
				threadsRan++;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		workers.clear();
	}
	//---------------------------------------------------------------------------------------------		
	// swapping the rivers // -------------------------------------------------------------------------------	
	//---------------------------------------------------------------------------------------------				

	private void swapRivers() {
		pop.sort(null);
		if (pop.get(0).type == streamType.River) {
			int index = findSea();
			pop.get(0).type = streamType.Sea;
			pop.get(index).type = streamType.River;
		}
	}

	public int findSea() {
		for (Stream s : pop) {
			if (s.type == streamType.Sea)
				return pop.indexOf(s);
		}
		return -1;
	}

	
	
	//---------------------------------------------------------------------------------------------		
	// WORKERS CLASSES // -------------------------------------------------------------------------------	
	//---------------------------------------------------------------------------------------------			
	
	private class StreamWorker extends Thread {//THIS WORKER moves the streams, one worker per river or sea to work on that river's or sea's streams
		public StreamWorker(Stream s) {
			super();
			this.s = s;
		}

		Stream s;

		public void run() {
			this.moveStream(s);
		}

		public void moveStream(Stream parent) {
			for (Stream s : parent.children) {
				try {
					Data start = s.data;
				
					S.acquire();
					
					double startValue = obj.value(start);
					S.release();
					Data data = start.randomSelectInNeighbour(1);
					S.acquire();
					double dataValue = obj.value(data);
					S.release();
					int tries = 0;
					while (dataValue > startValue &&  tries < MAX_TRIES) { // UN COMMENT THIS TO HELP WITH GETTING STUCK WHEN TOO HIGH TIMEOUT
						data = start.randomSelectInNeighbour(1);
						S.acquire();
						dataValue = obj.value(data);
						S.release();
						tries++;
					}/*
					if (dataValue > startValue)
					{
						System.out.println("this "+ startValue + " gets " + dataValue + " Bad Sample after " + tries + " tries");
					}
					else {
						System.out.println("this "+ startValue + " gets " + dataValue);
					}*/
					if (tries < MAX_TRIES) {
						s.data = data;
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			

		}

	}

	private class RiverWorker extends Thread {//this worker moves rivers, one thread per river
		public RiverWorker(Stream s) {
			super();
			this.s = s;
		}
		Stream s;

		public void run() {
			this.moveStream(s);
		}

		public void moveStream(Stream s) {
			try {
			Data start = s.data;
			S.acquire();
			double startValue = obj.value(start);
			S.release();
			Data data = start.randomSelectInNeighbour(1);
			S.acquire();
			double dataValue = obj.value(data);
			S.release();
			int tries = 0;
			while (dataValue > startValue && tries < MAX_TRIES) {
				data = start.randomSelectInNeighbour(1);
				S.acquire();
				dataValue = obj.value(data);
				S.release();
				tries++;
			}
			/* DEBBUGING CODE
			if (dataValue > startValue)
			{
				System.out.println("this "+ startValue + " gets " + dataValue + " Bad Sample after " + tries + " tries");
			}
			else {
				System.out.println("this "+ startValue + " gets " + dataValue);
			}
			*/
			if (tries < MAX_TRIES) {
				s.data = data;
			}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		

	}

	@Override
	public void optimize() {
		
		
		System.out.println("Start system : \n" + toString());
		Date d = new Date();
		int i = 0;
		long startTime = System.currentTimeMillis();

		while (System.currentTimeMillis() - startTime < maxTime) {
			moveStreams();
			swapStreams();
			moveRivers();
			swapRivers();
			i++;
			//System.out.println("iteration " + i);
			//System.out.println(toString());//UNCOMMENT IF YOU WANT TO SEE PROGRESSS
		}
		/*UNCOMMENT THIS AND COMMENT ON TOP TO NOT USE TIMER BUT ITERATIONS AND CHOOOSE NUMBER OF ITERATIONS WITH THE FOR CONDITIONS
		for (i = 0;i < 300;i++) {
			moveStreams();
			swapStreams();
			moveRivers();
			swapRivers();
			//System.out.println(toString());//UNCOMMENT IF YOU WANT TO SEE PROGRESSS
		}
		*/
		System.out.println("End system : \n" + toString() + "\n With " + i + " Iterations in " + maxTime + " miliseconds");
		
		

	}
	
	@Override
	public Data getSolution() {
		return pop.get(0).data; //messy
	}
	
	//---------------------------------------------------------------------------------------------		
	// STREAM CLASSSES // -------------------------------------------------------------------------------	
	//---------------------------------------------------------------------------------------------			
	enum streamType {
		River, Sea, Stream
	}

	public class Stream implements Comparable<Stream> {
		int id;
		boolean assigned = false;
		streamType type;
		int nAssignedStreams;
		LinkedList<Stream> children;
		Data data;

		public Stream(Data d) {
			id = newId();
			this.data = d;
			this.type = streamType.Stream;
			this.children = new LinkedList<Stream>();
			;
		}

		public Stream(streamType type, Data d) {
			this.type = type;
			this.data = d;
			this.children = new LinkedList<Stream>();
			;
		}

		public Stream(streamType type, Data d, LinkedList<Stream> children) {
			this.type = type;
			this.data = d;
			this.children = children;
		}

		public Stream(Stream s) {
			this.type = s.type;
			this.data = s.data;
			this.children = s.children;
		}

		@Override
		public int compareTo(Stream other) {
			if (obj.value(this.data) > obj.value(other.data)) {
				return 1;
			}
			if (obj.value(this.data) < obj.value(other.data)) {
				return -1;
			}
			return 0;
		}

		@Override
		public String toString() {
			return "[" + id + "\t" + type + ":      \t" + obj.value(data) + ", \t" + nAssignedStreams + ",\t" + children
					+ "]";
		}

	}

	@Override
	public String toString() {
		String s = "";
		for (Stream str : pop) {
			s = s + str.toString() + "\n";
		}
		return s;
	}

	public static void main(String[] args) {
		
		/*Objective obj = new ColorPartition(4, 14);
		WCAv2 bm = new WCAv2(obj.solutionSample(), obj, 10000);
		bm.optimize();*/
		int ITMAX = 20000;  // number of iterations

	      // BitCounter
	      int n = 50;
	      Objective obj = new BitCounter(n);
	      Data D = obj.solutionSample();
	      WCAv2 rw = new WCAv2(D,obj,ITMAX);
	      System.out.println(rw);
	      System.out.println("starting point : " + rw.getSolution());
	      System.out.println("optimizing ...");
	      rw.optimize();
	      System.out.println(rw);
	      System.out.println("solution : " + rw.getSolution());
	      System.out.println();

	      // Fermat
	      int exp = 2;
	      int ndigits = 10;
	      obj = new Fermat(exp,ndigits);
	      D = obj.solutionSample();
	      rw = new WCAv2(D,obj,ITMAX);
	      System.out.println(rw);
	      System.out.println("starting point : " + rw.getSolution());
	      System.out.println("optimizing ...");
	      rw.optimize();
	      System.out.println(rw);
	      System.out.println("solution : " + rw.getSolution());
	      Data x = new Data(rw.solution,0,ndigits-1);
	      Data y = new Data(rw.solution,ndigits,2*ndigits-1);
	      Data z = new Data(rw.solution,2*ndigits,3*ndigits-1);
	      System.out.print("equivalent to the equation : " + x.posLongValue() + "^" + exp + " + " + y.posLongValue() + "^" + exp);
	      if (rw.objValue == 0.0)
	         System.out.print(" == ");
	      else
	         System.out.print(" ?= ");
	      System.out.println(z.posLongValue() + "^" + exp);
	      System.out.println();

	      // ColorPartition
	      n = 4;  int m = 14;
	      ColorPartition cp = new ColorPartition(n,m);
	      D = cp.solutionSample();
	      rw = new WCAv2(D,cp,ITMAX);
	      System.out.println(rw);
	      System.out.println("starting point : " + rw.getSolution());
	      System.out.println("optimizing ...");
	      rw.optimize();
	      System.out.println(rw);
	      System.out.println("solution : " + rw.getSolution());
	      cp.value(rw.solution);
	      System.out.println("corresponding to the matrix :\n" + cp.show());
	}

}
//TODO add time limit
//add bounds
//
