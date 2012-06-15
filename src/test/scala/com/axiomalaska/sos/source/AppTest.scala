package com.axiomalaska.sos.source

import org.junit._
import Assert._
import com.axiomalaska.sos.source.stationupdater.RawsStationUpdater
import com.axiomalaska.sos.source.stationupdater.NdbcStationUpdater

@Test
class AppTest {

  @Test
  def testOK() = assertTrue(true)
  
//  @Test
//  def testMovePhenomena(){
//    val sensorbuilder = new QueryBuilder()
//    val queryBuilder = new StationQueryBuilder()
//
//    sensorbuilder.withSensorObservationQuery(sensorQuery => {
//      queryBuilder.withStationQuery(stationQuery => {
//        val phenomena = sensorQuery.getPhenomena()
//        val sorted = phenomena.sortWith((p1, p2) => p1.id < p2.id)
//    	  for(sensorPhenomenon <- sorted.tail){
//    	    val databasePhenomenon = new DatabasePhenomenon(sensorPhenomenon.tag, sensorPhenomenon.units, sensorPhenomenon.description, sensorPhenomenon.name)
//    	    stationQuery.createPhenomenon(databasePhenomenon)
//    	  }
//      })
//    })
//  }
  
  @Test
  def updateSource(){
    val queryBuilder = new StationQueryBuilder()

    queryBuilder.withStationQuery(stationQuery => {
      val stationUpdater = new NdbcStationUpdater(stationQuery, None)
      
      stationUpdater.update()
    })
  }
}

