package strategies;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.snt.inmemantlr.tree.Ast;
import org.snt.inmemantlr.tree.AstNode;

import cppparser.Grammar;

public class BasicAlgorithm {
	private  Map<Ast, String> treeMap=new HashMap<>();
	private  Map<Integer,List<Ast>> bucketMap=new HashMap<>();
	private  Map<String, Map<AstNode,Integer>> initialMap=new HashMap<>();
	private  Map<String, Double> matchPart=new HashMap<>();
	/**
	 * getBucket(int nodeCount) calculates the bucket number for the astTree
	 * @param nodeCount number of ast nodes in a subtree
	 * @return
	 */
	public int getBucket(int nodeCount)
	{
		int bucket=0;

		if(nodeCount<10)
		{
			bucket=nodeCount;
		}
		else
		{   
			double bucket1=nodeCount*0.1;
			bucket=(int)bucket1;
		}
		return bucket;
	}
	/**
	 * 
	 * @param bucketNumber it represents the bucket in which subtree is present
	 * @param subAst it gives the list of subtrees
	 * @param list it represents a list of Asts
	 */
	public Map<Integer,List<Ast>> populateBucketMap(int bucketNumber, Set<Ast> subAst, List<Ast> list, String fileName)
	{
		for(Ast a :subAst)
		{   
			int hash=a.hashCode()%bucketNumber;
			if(bucketMap.containsKey(hash))
			{
				list=bucketMap.get(hash);
				list.add(a);
				bucketMap.put(hash, list);
			}
			else
			{
				list=new ArrayList<>();
				list.add(a);
				bucketMap.put(hash, list);     		
			}
			treeMap.put(a, fileName);
		}
		return bucketMap;
	}
	/**
	 * 
	 * @param ok it is a list of C files to be given as input
	 * @param gfile it is a syntax parser file for C+
	 */
	public Map<String, Map<AstNode,Integer>> populateInitialMatch(Map<String, Ast> map)
	{
		List<Ast> list=new ArrayList<>();
		Map<AstNode,Integer> m;
		/**
		 * Using hashing to classify subtrees to buckets
		 */
		for(Map.Entry<String, Ast> entry: map.entrySet())
		{   
			m=new HashMap<>();
			Predicate<AstNode> pred=p->p.getChildren()!=null;
			int n=entry.getValue().getNodes().size();
			int bucketNumber=getBucket(n);
			Set<Ast> subAst=entry.getValue().getSubtrees(pred);
			for(AstNode n1 : entry.getValue().getNodes())
			{   
				m.put(n1, 0);
			}
			initialMap.put(entry.getKey(),m);
			bucketMap=populateBucketMap(bucketNumber,subAst, list, entry.getKey());
		}
		return initialMap;
	}


	/**
	 * This method populates result for all matching Ast subtrees
	 * @param firstAst
	 * @param secondAst
	 * @param firstFile
	 * @param secondFile
	 * @param result
	 */
	public void populateResult(Ast firstAst, Ast secondAst, List<Ast> firstFile, List<Ast> secondFile, Map<String, List<Ast>> result)
	{
		if(result.containsKey(treeMap.get(firstAst)))
		{
			firstFile=result.get(treeMap.get(firstAst));
			firstFile.add(firstAst);
			result.put(treeMap.get(firstAst),firstFile);
		}
		else
		{
			firstFile=new ArrayList<>();
			firstFile.add(firstAst);
			result.put(treeMap.get(firstAst),firstFile);
		}

		if(result.containsKey(treeMap.get(secondAst)))
		{
			secondFile=result.get(treeMap.get(secondAst));
			secondFile.add(secondAst);
			result.put(treeMap.get(secondAst),secondFile);
		}
		else
		{
			secondFile=new ArrayList<>();
			secondFile.add(secondAst);
			result.put(treeMap.get(secondAst),secondFile);
		}
	}

	/**
	 * This algorithm calculates the subtree match
	 * @param map it has file names and corresponding AST
	 * @param initialMap it contains fileNames with corresponding AST's and their match count
	 * @param bucketMap it contains bucket number and AST lists
	 * @param treeMap it contains tree/subtrees with corresponding fileName
	 */
	public Map<List<String>,List<Double>>  basicPlagiarism(File[] files, File cppConfigFile)
	{
		Map<String, Ast> map=Grammar.runAndGetAsts(files, cppConfigFile);
		initialMap=populateInitialMatch(map);
		List<String> pairs=new ArrayList<>();
		Map<String, List<Ast>> result=new HashMap<>();
		Map<String, Integer> counter=new HashMap<>();
		Map<List<String>, Map<String, List<Ast>>>  finalResult=new HashMap<>();
		List<Ast> firstFile=new ArrayList<>();
		List<Ast> secondFile=new ArrayList<>();
		Map<List<String>,List<Double>> percentResult=new HashMap<>();
		/**
		 * Initialize counter
		 */
		for(Map.Entry<String, Ast> k:map.entrySet())
		{
			counter.put(k.getKey(), 0);
		}
		for(Map.Entry<Integer, List<Ast>>  entry: bucketMap.entrySet())
		{   
			if(entry.getValue().size()>=2)
			{  
				List<Ast> newList=new ArrayList<>(entry.getValue());
				for(Ast firstAst: entry.getValue())
				{					
					newList.remove(firstAst);
					for(Ast secondAst:newList)
					{   
						int num1=firstAst.getNodes().size();
						int count1=0;
						if(secondAst.getNodes().size()==num1 && treeMap.containsKey(secondAst) && treeMap.containsKey(firstAst) 
								&& (treeMap.get(secondAst)!=treeMap.get(firstAst)))
						{   				

							count1=getSimilarSubtrees(firstAst,secondAst, count1,counter,pairs);
						}

						if(num1==count1)
						{   
							populateResult(firstAst,secondAst,firstFile, secondFile,result);
							finalResult.put(pairs,result);
							calculateResult(finalResult,map,percentResult);
							
						}
					}
				}
			}
		}
		return percentResult;
	}
	/**
	 * 
	 * @param firstAst
	 * @param secondAst
	 * @param count1
	 * @param counter
	 * @param pairs
	 * @return
	 */
	public int getSimilarSubtrees(Ast firstAst, Ast secondAst, int count1,Map<String, Integer> counter,List<String> pairs)
	{
		for(AstNode node: firstAst.getNodes())
		{   
			for(AstNode s1:secondAst.getNodes())
			{    
				if( s1.toString().replaceAll("^[0-9]+", "").equals( node.toString().replaceAll("^[0-9]+", "")))
				{   
					Map<AstNode, Integer> n=initialMap.get(treeMap.get(secondAst));
					count1++;
					if(n.get(s1)!=1)
					{
						n.put(s1, 1);											
						initialMap.put(treeMap.get(secondAst), n);
					}
					if(counter.containsKey(treeMap.get(secondAst)))
						counter.put(treeMap.get(secondAst), counter.get(treeMap.get(secondAst))+1);
					Map<AstNode, Integer> w=initialMap.get(treeMap.get(firstAst));
					if(w.get(node)!=1)
					{   
						w.put(node, 1);
						initialMap.put(treeMap.get(firstAst), w);
					}
					if(counter.containsKey(treeMap.get(firstAst)))
						counter.put(treeMap.get(firstAst), counter.get(treeMap.get(firstAst))1);
					pairs=new ArrayList<>();
					pairs.add(treeMap.get(secondAst));
					pairs.add(treeMap.get(firstAst));

				}
			}
		}
		return count1;
	}
	/**
	 * This method calculates code for sequence plagiarism detection
	 * @param finalResult
	 * @param stringMap
	 */
	public void SequenceDetection(Map<List<String>, Map<String, List<Ast>>> finalResult, Map<String, Ast> stringMap)
	{
		for(Map.Entry<List<String>, Map<String, List<Ast>>> map: finalResult.entrySet())
		{
			Map<String, List<Ast>> res=map.getValue();
			for(Map.Entry<String, List<Ast>> result: res.entrySet())
			{
				List<Ast> subtreeList=result.getValue();
				List<Ast> list=new ArrayList<>();

				Comparator<Ast> astLengthComparator = new Comparator<Ast>()
				{
					@Override
					public int compare(Ast o1, Ast o2)
					{
						return Integer.compare(o2.getNodes().size(), o1.getNodes().size());
					}
				};

				Collections.sort(subtreeList, astLengthComparator);
				list.add(subtreeList.get(0));

				for(Ast firstAst: subtreeList)
				{  
					List<AstNode> parent=firstAst.getNodes();
					int count=0;
					for(Ast a :list)
					{  
						List<AstNode> l=a.getNodes();
						for(AstNode p:parent)
						{
							for(AstNode s: l)
							{
								if(s.toString().equals(p.toString()))
								{									
									count++;								
								}
							}
						}
					}
					if(count>=0 && count!=parent.size())
					{
						list.add(firstAst);
					}					

				}

				int nodeCount=stringMap.get(result.getKey()).getNodes().size();
				int count=0;
				for(Ast a :list)
				{
					count=count+a.getNodes().size();
				}
				double percent=((double)count/nodeCount)*100;
				matchPart.put(result.getKey(), percent);
			}
		}

	}
	/**
	 * This method calculates result for percentage of plagiarism
	 * @param finalResult
	 * @param stringMap
	 */
	public void calculateResult(Map<List<String>, Map<String, List<Ast>>> finalResult, Map<String, Ast> stringMap,Map<List<String>,List<Double>> percentResult)
	{
		for(Map.Entry<List<String>, Map<String, List<Ast>>> map: finalResult.entrySet())
		{
			Map<String, List<Ast>> res=map.getValue();
			List<Double> percentage=new ArrayList<>();
			List<String> files=new ArrayList<>();
			for(Map.Entry<String, List<Ast>> result: res.entrySet())
			{
				List<Ast> subtreeList=result.getValue();
				Set<AstNode> setNode=new HashSet<>();
				for(Ast tree:subtreeList)
				{
					for(AstNode astNode: tree.getNodes())
					{
						setNode.add(astNode);
					}
				}
				int nodeCount=stringMap.get(result.getKey()).getNodes().size();
				double percent=((double)setNode.size()/nodeCount)*100;
				matchPart.put(result.getKey(), percent);
				percentage.add(percent);
				files.add(result.getKey());
			}
			for(String s: files)
				System.out.println(s);
			percentResult.put(files,percentage);
			
		}
	}
}