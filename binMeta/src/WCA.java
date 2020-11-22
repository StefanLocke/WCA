import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class WCA extends binMeta {

	
	public final int C = 2;
	public WCA(Data startPoint, Objective Obj, long maxTime) {

		this.metaName = "Water cycle Algorithm";
		this.solution = startPoint;
		this.obj = Obj;
		this.maxTime = maxTime;
		this.objValue = this.obj.value(this.solution);
		
	}

	int numberOfSeaAndRivers;
	int numberOfTotal;
	List<Stream> pop;

	@Override
	public void optimize() {
		// TODO Auto-generated method stub

	}

	public void generate(int numberOfSeaAndRivers, int numberOfTotal, double dMax) {
		this.numberOfSeaAndRivers = numberOfSeaAndRivers;
		this.numberOfTotal = numberOfTotal;
		pop = new LinkedList<Stream>();
		for (int i = 0; i < numberOfTotal; i++) {
			pop.add(new Stream(obj.solutionSample()));
		}
		pop.sort(null);
		for (int i = 0; i < numberOfSeaAndRivers; i++) {
			if (i == 0) {
				Stream tmp = pop.remove(i);
				tmp = tmp.convertToSea();
				pop.add(i, tmp);
			} else {
				Stream tmp = pop.remove(i);
				tmp = tmp.convertToRiver();
				pop.add(i, tmp);
			}
		}

	}

	public String toString() {
		String st = "";
		for (Stream s : pop) {
			st = st + s.toString() + "\n";
		}
		return st;
	}
	int nbOfdesignatedStreams = 0;
	public void designateAll() {
		for (Stream s : pop) {
			if (s instanceof RiverOrSea) {
				RiverOrSea se = (RiverOrSea)s;
				int i = designateStreams((RiverOrSea)s);
				se.nOfStreams = i;
				nbOfdesignatedStreams = nbOfdesignatedStreams + i;
			}
		}
		
		while (nbOfdesignatedStreams < numberOfTotal - numberOfSeaAndRivers) {
			Random r = new Random();
			RiverOrSea rv = (RiverOrSea) pop.get(r.nextInt(numberOfSeaAndRivers));
			rv.nOfStreams ++;
			nbOfdesignatedStreams++;
		}
		
		while (nbOfdesignatedStreams > numberOfTotal - numberOfSeaAndRivers) {
			Random r = new Random();
			RiverOrSea rv = (RiverOrSea) pop.get(r.nextInt(numberOfSeaAndRivers));
			if (rv.nOfStreams > 0) {
				rv.nOfStreams --;
				nbOfdesignatedStreams--;
			}
		}
	}
	
	
	private void streamRiverSea(Stream s, Stream r) {
		System.out.println(s + " " + r);
		Random rand = new Random();
		double d = s.dataValue + (rand.nextDouble() * C * (r.dataValue - s.dataValue) );
		System.out.println(d);
		s.data = getDataOfValue(d, 1.0);
		s.dataValue = obj.value(s.data);
		if (s.dataValue < r.dataValue) {
			switchStreamToOther(s, r);
		}
	}
	
	
	public Data getDataOfValue(double value,double threshold) {
		Data d = obj.solutionSample();
		while (Math.abs(obj.value(d) - value) < threshold) {
			d = obj.solutionSample();
			threshold = threshold *0.01;
		}		
		return d;
	}
	
	
	public void switchStreamToOther(Stream s, Stream rs) {
		Stream tmps = new Stream(rs.data);
		tmps.ParentStream = rs.ParentStream;
		
		River casted = (River) rs;
		River tmprs = new River(s.data);
		tmprs.ParentStream = rs.ParentStream;
		tmprs.ChildrenStreams = casted.ChildrenStreams;
		tmprs.nOfStreams = casted.nOfStreams;
		pop.remove(s);
		pop.add(tmps);
		pop.remove(rs);
		pop.add(tmprs);
	}
	public void streamsToRiversAndSea() {
		
	}
	
	
	
	public int designateStreams(RiverOrSea rs) {
		double Cn = rs.dataValue - pop.get(numberOfSeaAndRivers).dataValue;
		double sum = 0;
		for (int i = 0; i < numberOfSeaAndRivers ; i++) {
			sum = sum + (pop.get(i).dataValue - pop.get(numberOfSeaAndRivers).dataValue ) ;
		}
		System.out.println(Cn+ "/" +sum);
		double bfore = Math.abs(Cn/sum) * (numberOfTotal - numberOfSeaAndRivers);
		System.out.println("before : " + bfore);
		long NSn = Math.round(bfore);
		System.out.println("after : " + NSn);
		return (int)NSn;
	}
	private int currentRiver;
	
	public void nextRiver() {
		if (currentRiver >= numberOfSeaAndRivers -1) {
			currentRiver = -1;
		}
		else {
			currentRiver++;
		}
	}
	
	
	
	public void assignRandomUnassignedTo(RiverOrSea r) {
		List<Stream> onlyStreams = new LinkedList<>(pop);
		
		Iterator<Stream> it = onlyStreams.iterator();
		while(it.hasNext()) {
			Stream s = it.next();
			if (s instanceof RiverOrSea) {
				it.remove();
			}
			else {
				if (s.ParentStream != null) {
					it.remove();
				}
			}
		}
		
		
		
		Random rand = new Random();
		
		while (r.ChildrenStreams.size() < r.nOfStreams) {
			int i = rand.nextInt(onlyStreams.size());
			onlyStreams.get(i).ParentStream = r;
			r.ChildrenStreams.add(onlyStreams.remove(i));
		}
		
	}
	
	public void assign() {
		for (Stream v : pop) {
			if (v instanceof RiverOrSea) {
				v.ParentStream = pop.get(0);
				assignRandomUnassignedTo((RiverOrSea)v);
			}			
		}
	}
	
	
	public static double StreamCost(Stream s ) {
		
		return s.dataValue;
	}
	
	public class Stream implements Comparable<Stream> {
		Data data;
		Stream ParentStream;
		double dataValue;

		public Stream(Data data) {
			this.data = data;
			dataValue = obj.value(data);
			ParentStream = null;
		}

		public Stream convertToStream() { //TODO optimise to not recalculate objvalue each time we convert 
			return new Stream(this.data);
		}

		public Stream convertToRiver() {
			return new River(this.data);
		}

		public Stream convertToSea() {
			return new Sea(this.data);
		}
		
		
		
		
		

		@Override
		public int compareTo(Stream arg0) {
			if (this.dataValue > arg0.dataValue) {
				return 1;
			}
			if (this.dataValue < arg0.dataValue) {
				return -1;
			}
			return 0;
		}

		public String toString() {
			return "[Stream : " + dataValue + " ]";
		}
	}
	
	public abstract class RiverOrSea extends Stream {
		public int nOfStreams;
		public List<Stream> ChildrenStreams;
		public RiverOrSea(Data data) {
			super(data);
			nOfStreams = 0;
			ChildrenStreams = new LinkedList<>();
		}
	}

	public class River extends RiverOrSea {
		
		public River(Data data) {
			super(data);
		}

		public String toString() {
			return "[River :" + dataValue+ ", "+ nOfStreams + "," + ChildrenStreams.toString()+ "]";
		}
	}

	public class Sea extends RiverOrSea {
		public Sea(Data data) {
			super(data);
		}

		public String toString() {
			return "[Sea :" + dataValue + ", "+ nOfStreams + "," + ChildrenStreams.toString()+ "]";
		}
	}

	public static void main(String[] args) {
		Objective obj = new BitCounter(20);
		WCA bm = new WCA(obj.solutionSample(), obj, 100);
		bm.generate(10,20,0.0);
		bm.designateAll();
		bm.assign();
		System.out.println(bm.toString());
		bm.streamRiverSea(bm.pop.get(15), bm.pop.get(15).ParentStream);
		System.out.println(bm.toString());
	}
}
