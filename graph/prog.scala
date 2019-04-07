import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import scala.collection.mutable.ListBuffer


def makeEdges(line: String): Array[Edge[Double]] =
{
    var edges = new ListBuffer[Edge[Double]]()
    val fields = line.split(":")
    val origin = fields(0)
    val others = fields(1).replace("(", "").replace(")", "").split(", ")
    others.foreach { item => edges += Edge(origin.toLong, item.split(" ")(0).toLong, item.split(" ")(1).toDouble) }
    
    edges.toArray
}


def findMinEdge(edge1 : Edge[Double], edge2 : Edge[Double]) : Edge[Double] =
{
    if(edge1.attr < edge2.attr) edge1
    else if (edge1.attr == edge2.attr)
    {
        if(edge1.srcId < edge2.srcId) edge1
        else if(edge1.srcId == edge2.srcId)
        {
            if(edge1.dstId < edge2.dstId) edge1
            else edge2
            
        }
        else edge2
    }
    else
        edge2
}


def run(graph: Graph[Long,Double]) : Graph[Long,Double] =
{
    var emptyEdges = graph.edges.filter(edge => edge.srcId == -1)

    val keyGenEdges = graph.edges.map( edge => (edge.srcId, edge))
    val opositKeyGenEdges = graph.edges.map( edge => (edge.dstId, edge))
    var unionEdges = keyGenEdges union opositKeyGenEdges

    // val keyGenEdges = remainingEdges2.map( edge => (edge.srcId, edge))
    // val opositKeyGenEdges = remainingEdges2.map( edge => (edge.dstId, edge))
    // var unionEdges = keyGenEdges union opositKeyGenEdges

    var verts = graph.vertices

    // while ...
    val minEdges = verts.join(unionEdges).map{case (vKey, (vGrp, edge)) => (vGrp, edge)}.reduceByKey((edge1, edge2) => findMinEdge(edge1, edge2))
    val minEdgesDistinct = minEdges.values.distinct
    val unionMinEdges = minEdgesDistinct.map(edge => (edge.srcId, edge.dstId)) union minEdgesDistinct.map(edge => (edge.dstId, edge.srcId))
    // val unionMinEdges = unionMinEdges.distinct

    val tmp = unionMinEdges.map{case (s, d) => (s, math.min(s, d))} union unionMinEdges.map{case (s, d) => (s, math.max(s, d))}
    var check = 1L
    while (check != 0)
    {
        val newGrp = verts.join(tmp).map{case (key, (grp, vert)) => (vert, grp)}
        val newGrp1 = newGrp.distinct
        val newGrp2 = newGrp1.reduceByKey((grp1, grp2) => math.min(grp1, grp2))
        val newVerts = verts.join(newGrp2).map{case (vertID, (oldGrp, newGrp)) => (vertID, newGrp)}
        check = verts.join(newVerts).filter{case (vert, (oldGrp, newGrp)) => oldGrp != newGrp}.count
        println(check)
        verts = VertexRDD(verts.subtractByKey(newVerts) union newVerts)
    }
    verts.collect
    emptyEdges = emptyEdges ++ minEdgesDistinct
    unionEdges = unionEdges.subtract(minEdges)

    val remainingEdges = unionEdges.values.distinct.map(edge => (edge.srcId, edge)).join(verts).map{case (vertID, (edge, gr)) => (edge.dstId, (edge, gr))}
    val remainingEdges1 = remainingEdges.join(verts).map{case (vertID, ((edge, gr1), gr2)) => (edge, gr1, gr2)}
    val remainingEdges2 = remainingEdges1.filter{case (edge, gr1, gr2) => gr1 != gr2}.map{case (edge, gr1, gr2) => edge}
    // unionEdges.join(verts).map{case (vertID, (edge, gr)) => (edge.dstId, (edge, gr))}.join(verts).map{case (vertID, ((edge, gr1), gr2)) => (edge, gr1, gr2)}.filter{case (edge, gr1, gr2) => gr1 != gr2}.map{case (edge, gr1, gr2) => edge}
}


val file = sc.textFile("/mnt/f/prog/5-course/graph/test_graph_not_binary_1");
val edgesForGraph = file.flatMap(line => makeEdges(line))
val vertsForGraph = file.flatMap(line => Array((line.split(":")(0).toLong, line.split(":")(0).toLong)))
// val vertsForGraph = file.flatMap(line => Array((line.split(":")(0).toLong, line.split(":")(0).toLong + 1)))
// val graph = Graph(vertsForGraph, edgesForGraph)
// val graph = Graph(vertsForGraph, edgesForGraph).partitionBy(PartitionStrategy.RandomVertexCut)

val vertsForGraphVR: VertexRDD[Long] = VertexRDD(vertsForGraph)
val edgesForGraphER: EdgeRDD[Double] = EdgeRDD.fromEdges(edgesForGraph)
val graph = Graph(vertsForGraphVR, edgesForGraphER).partitionBy(PartitionStrategy.RandomVertexCut)




graph.triplets.collect.foreach(println)



for group => findMinEdge, groupBy, makeExternalEdges
