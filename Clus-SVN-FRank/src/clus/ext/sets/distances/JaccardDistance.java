package clus.ext.sets.distances;
import java.util.Arrays;

import clus.data.type.SetAttrType;
import clus.ext.sets.Set;
import clus.ext.sets.SetDistance;
import clus.statistic.ClusDistance;


public class JaccardDistance extends SetDistance{

	
	public JaccardDistance(SetAttrType attr, ClusDistance innerDistance) {
		super(attr,innerDistance);
	}

	public JaccardDistance(ClusDistance innerDistance) {
		super(innerDistance);
	}
	
	@Override
	public double calcDistance(Set set1, Set set2) {
		 ClusDistance clusDistance = childDistances[0];
		double union=set1.getValues().length+set2.getValues().length;
		double intersection = 0;
		for (Object element1 : set1.getValues()) {			
			double dist=1;
			for (Object element2 : set2.getValues()) {		
				if (clusDistance==null){
					dist=Math.abs((Double)element1-(Double)element2);
				}else{
					dist=clusDistance.calcDistance(element1, element2);
				}
				if (dist==0){
					intersection++;			
					union--;
				}
			}
		}


		long timeEnd = System.currentTimeMillis();
		
		//System.out.println("To calculate distance between two sets:\t" + (timeEnd-timeStart)+ " miliseconds.");
		
		return 1-(intersection/union);
	}
}
