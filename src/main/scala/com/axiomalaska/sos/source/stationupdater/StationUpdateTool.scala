package com.axiomalaska.sos.source.stationupdater

import org.apache.log4j.Logger
import com.axiomalaska.sos.source.data.DatabasePhenomenon
import com.axiomalaska.sos.source.data.DatabaseSensor
import com.axiomalaska.sos.source.data.DatabaseStation
import com.axiomalaska.sos.source.data.ObservedProperty
import com.axiomalaska.sos.source.data.Source
import com.axiomalaska.sos.source.StationQuery

class StationUpdateTool(private val stationQuery:StationQuery) {
  private val log = Logger.getRootLogger()
  
  // ---------------------------------------------------------------------------
  // Public Members
  // ---------------------------------------------------------------------------
  
  def updateStations(sourceStationSensors: List[(DatabaseStation, 
      List[(DatabaseSensor, List[DatabasePhenomenon])])], 
      databaseStations: List[DatabaseStation]){

    for (sourceStationSensor <- sourceStationSensors) {
      val sourceStation = sourceStationSensor._1
      val sourceSensors = sourceStationSensor._2.distinct

      databaseStations.filter(databaseStation => 
        databaseStation.name == sourceStation.name && 
          databaseStation.foreign_tag == sourceStation.foreign_tag && 
          areClose(databaseStation.latitude, sourceStation.latitude) && 
          areClose(databaseStation.longitude, sourceStation.longitude)).headOption match {
        case Some(databaseStation: DatabaseStation) => {
          if (!areDoublesEquals(databaseStation.latitude, sourceStation.latitude) || 
              !areDoublesEquals(databaseStation.longitude, sourceStation.longitude)) {
            stationQuery.updateStation(databaseStation, sourceStation)
            log.info("Updating Station " + databaseStation.name)
          }

          val databaseSenors = stationQuery.getSensors(databaseStation)
          for((sourceSensor, phenomena) <- sourceSensors){
            if (!isThereAnEqualSensor(databaseSenors, sourceSensor, phenomena)) {

              val createdSensor = stationQuery.createSensor(databaseStation, sourceSensor)
              phenomena.foreach(phenomenon => 
                stationQuery.associatePhenomonenToSensor(createdSensor, phenomenon))
                
              stationQuery.associateSensorToStation(databaseStation, createdSensor)
              log.info("Association Sensor " + sourceSensor.description +
                " to Station " + sourceStation.name)
            }
          }
        }
        case None => {
          val databaseStation = stationQuery.createStation(sourceStation)

          for ((sourceSensor, phenomena) <- sourceSensors) {
            val createdSensor = stationQuery.createSensor(databaseStation, sourceSensor)
            phenomena.foreach(phenomenon =>
              stationQuery.associatePhenomonenToSensor(createdSensor, phenomenon))
            stationQuery.associateSensorToStation(databaseStation, createdSensor)
          }
        }
      }
    }
  }
  
  def getSourceSensors(station:DatabaseStation, observedProperties: List[ObservedProperty]): 
  List[(DatabaseSensor, List[DatabasePhenomenon])] = {

    val sensors = observedProperties.map(observedProperty => {
      val phenomenon = stationQuery.getPhenomenon(observedProperty.phenomenon_id)
      (new DatabaseSensor("", "", station.id, observedProperty.depth), List(phenomenon))
    })

    return sensors
  }
  
  def updateObservedProperties(source: Source,
    sourceObservedProperies: List[ObservedProperty]): List[ObservedProperty] = {

    val observedProperties = for(observedProperty <- sourceObservedProperies) yield {
      stationQuery.getObservedProperty(observedProperty.foreign_tag, source) match {
        case Some(databaseObservedProperty) => {
          stationQuery.updateObservedProperty(databaseObservedProperty, observedProperty)
          stationQuery.getObservedProperty(observedProperty.foreign_tag, source).get
        }
        case None => {
          val newObservedProperties = stationQuery.createObservedProperty(observedProperty)
          log.info("creating new observedProperties " + observedProperty.foreign_tag)
          
          newObservedProperties
        }
      }
    }
      
    return observedProperties
  }
  
  // ---------------------------------------------------------------------------
  // Private Members
  // ---------------------------------------------------------------------------
  
  def createObservedProperty(foreignTag:String, source:Source, 
      foreignUnits:String, phenomenonId:Long, depth:Double = 0):ObservedProperty ={
    
    new ObservedProperty(foreignTag, source.id, foreignUnits,
      phenomenonId, depth)
  }
  
  private def isThereAnEqualSensor(originalSensors:List[DatabaseSensor], 
      newSensor:DatabaseSensor, newPhenomena:List[DatabasePhenomenon]):Boolean ={
    originalSensors.exists(originalSensor => {
       val originalPhenomena = stationQuery.getPhenomena(originalSensor)
       
       (newPhenomena.forall(newPhenomenon => originalPhenomena.exists(_.tag == newPhenomenon.tag)) &&
       originalPhenomena.forall(originalPhenomenon => newPhenomena.exists(_.tag == originalPhenomenon.tag)) &&
       originalSensor.depth == newSensor.depth)
    })
  }
  
  private def areClose(value1:Double, value2:Double):Boolean = {
    val roundedValue1 = value1.round
    val roundedValue2 = value2.round
    return roundedValue1 == roundedValue2
  }
  
  private def areDoublesEquals(value1:Double, value2:Double):Boolean = {
    val roundedValue1 = (value1 * 1000).round 
    val roundedValue2 = (value2 * 1000).round
    return roundedValue1 == roundedValue2
  }
}