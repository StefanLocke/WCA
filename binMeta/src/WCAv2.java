import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;




public class WCAv2 extends binMeta {

	
	public WCAv2(Data startPoint, Objective Obj, long maxTime) {
		idcount = 0;
		this.metaName = "Water cycle Algorithm";
		this.solution = startPoint;
		this.obj = Obj;
		this.maxTime = maxTime;
		this.objValue = this.obj.value(this.solution);
		
	}
	private int idcount;
	private int Nsr;
	private int Npop;
	List<Stream>pop;
	
	public int newId() {
		return idcount++;
	}
	
	
	public void generate(int numberOfSeaAndRivers, int numberOfTotal, double dMax) {
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
	private int NstreamsDesignated;
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
			rv.nAssignedStreams ++;
			NstreamsDesignated++;
		}
		
		while (NstreamsDesignated > Npop - Nsr) {
			Random r = new Random();
			Stream rv = pop.get(r.nextInt(Nsr));
			if (rv.nAssignedStreams > 0) {
				rv.nAssignedStreams --;
				NstreamsDesignated--;
			}
		}
	}
	public int designateStreams(Stream rs) {
		double Cn = obj.value(rs.data) - obj.value(pop.get(Nsr).data);
		double sum = 0;
		for (int i = 0; i < Nsr ; i++) {
			sum = sum + (obj.value(pop.get(i).data) - obj.value(pop.get(Nsr).data) ) ;
		}
		double bfore = Math.abs(Cn/sum) * (Npop - Nsr);
		long NSn = Math.round(bfore);
		return (int)NSn;
	}
	
	
	private void assignRandomUnassignedTo(Stream r) {
		List<Stream> onlyStreams = new LinkedList<>(pop);
		
		Iterator<Stream> it = onlyStreams.iterator();
		while(it.hasNext()) {
			Stream s = it.next();
			if (s.type == streamType.River || s.type == streamType.Sea) {
				it.remove();
			}
			else {
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
	
	private double getDistance(Stream s1, Stream s2) {
		return Math.abs(obj.value(s1.data) - obj.value(s2.data));
	}
	
	public Data getDataOfValue(double value,double threshold) {
		Data d = obj.solutionSample();
		while (Math.abs(obj.value(d) - value) < threshold) {
			d = obj.solutionSample();
			threshold = threshold *0.01;
		}		
		return d;
	}
	
	/*private void swapStreams(Stream s,Stream o) {
		Stream tmp = new Stream(s);
		s.children = o.children;
		s.children.remove(s);
		s.children.add(o);
		s.nAssignedStreams = o.nAssignedStreams;
		s.parent = o.parent;
		s.type = o.type;
		
		o.children = tmp.children;
		o.nAssignedStreams = tmp.nAssignedStreams;
		o.parent = tmp.parent;
		o.type = tmp.type;
	}
	private void swapStreamsWithSea(Stream s,Stream o) {
		Stream tmp = new Stream(s);
		s.children = o.children;
		s.nAssignedStreams = o.nAssignedStreams;
		s.parent = o.parent;
		s.type = o.type;
		
		o.children = tmp.children;
		o.nAssignedStreams = tmp.nAssignedStreams;
		o.parent = tmp.parent;
		o.type = tmp.type;
	}*/
	
	
	
	
	private void moveStream(Stream s) {
		/*Random rand = new Random();
		double d = obj.value(s.data) + (rand.nextDouble() * 2 * (obj.value(river.data) - obj.value(s.data)) );
		Data newdata = getDataOfValue(d, 0.1);
		s.data = newdata;*/
		Data start = s.data;
		Data data = start.randomSelectInNeighbour(2);
		while (obj.value(data) > obj.value(s.data)) {
			data = start.randomSelectInNeighbour(2);
		}
		s.data = data;
	}
	
	public void moveStreams() {
		for (Stream s:pop) {
			if (s.type == streamType.River || s.type == streamType.Sea) {
				for (Stream strm :s.children) {
					moveStream(strm);
				}
			}
		}
	}
	
	public void swapStreams(Stream river) {
		Data tmpdata = river.children.get(0).data; 
		river.children.getFirst().data = river.data;
		river.data = tmpdata;
	}
	
	public void swapStreams() {
		for (Stream s:pop ) {
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
	
	private void moveRivers(Stream river, Stream sea) {
		/*Random rand = new Random();
		double d = obj.value(river.data) + (rand.nextDouble() * 2 * (obj.value(sea.data) - obj.value(river.data)) );
		Data newdata = getDataOfValue(d, 0.1);
		river.data = newdata;*/
		Data start = river.data;
		Data data = start.randomSelectInNeighbour(2);
		while (obj.value(data) > obj.value(river.data)) {
			data = start.randomSelectInNeighbour(2);
		}
		river.data = data;
	}
	
	
	private void moveRivers() {
		for (Stream s:pop) {
			if (s.type == streamType.River) {
				moveRivers(s, pop.get(0));
			}
		}
	}
	
	
	private void swapRivers() {
		pop.sort(null);
		if (pop.get(0).type == streamType.River ) {
			int index = findSea();
			pop.get(0).type = streamType.Sea;
			pop.get(index).type = streamType.River;
		}
	}
	
	public int findSea() {
		for (Stream s:pop) {
			if (s.type == streamType.Sea)
				return pop.indexOf(s);
		}
		return -1;
	}
	
	
	
	
	
	
	
	
	
	@Override
	public void optimize() {
		// TODO Auto-generated method stub
		
	}

	
	
	
	
	
	
	
	
	
	enum streamType {
		River,
		Sea,
		Stream
	}
	
	
	public class Stream implements Comparable<Stream>{
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
			this.children = new LinkedList<Stream>();;
		}
		public Stream(streamType type,Data d) {
			this.type = type;
			this.data = d;
			this.children = new LinkedList<Stream>();;
		}
		
		public Stream(streamType type,Data d,LinkedList<Stream> children) {
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
			return "[" +id + "\t"+ type + ":      \t" + obj.value(data) + ", \t"+ nAssignedStreams + ",\t" +children + "]";
		}
		
		
		
	}
	
	@Override
	public String toString() {
		String s = "";
		for (Stream str:pop) {
			s = s + str.toString() + "\n";
		}
		return s;
	}
	public static void main(String[] args) {
		Objective obj = new ColorPartition(20,20);
		WCAv2 bm = new WCAv2(obj.solutionSample(), obj, 100);
		bm.generate(10,20,0.0);
		bm.designateAll();
		bm.assign();
		System.out.println("Before all moving and swapping \n" + bm.toString());
		for (int i = 0;i < 100;i++) {
			System.out.println("Iteration " + i);
			bm.moveStreams();
			bm.swapStreams();
			bm.moveRivers();
			bm.swapRivers();
			System.out.println(bm.toString());
		}
		
	}
	
	
}
//TODO add time limit
//add bounds
//

