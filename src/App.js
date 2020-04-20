import React from "react";
import { Map, Marker, Popup, TileLayer } from "react-leaflet";
import { Icon } from "leaflet";
//import * as parkData from "./data/skateboard-parks.json";
import "./App.css";
var overpass = require("query-overpass")

export const icon = new Icon({
  iconUrl: "/skateboarding.svg",
  iconSize: [25, 25]
});

class App extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      activeMapFeature: null,
      searchedPlace: "",
      mapData: null,
      mapCenter: [0.0, 0.0]
    };
    this.handleChange = this.handleChange.bind(this);
    this.searchPlace = this.searchPlace.bind(this);
    this.callback = this.callback.bind(this);
  }

  handleChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    this.setState({
      [name]: value
    });
  }

  callback(error, data){
    if(error){
      alert("error: " + error);
    }
    else{
      //alert("data: " + JSON.stringify(data));
      
      this.setState({
        mapData: data
      });
    }
  }

  searchPlace(event){
    overpass("[out:json];node(57.7,11.9,57.8,12.0)[amenity=bar];out;", this.callback);
    //query_overpass("node(51.249,7.148,51.251,7.152)[amenity=post_box];out;");
  }

  render(){
    return (
      <div>
        <div>
          <input type="textbox" onChange={this.handleChange} name="searchedPlace"></input>
          <button onClick = {this.searchPlace}>Search!</button>
        </div>
          <label>Searched Place: </label>
          <label>{this.state.searchedPlace}</label>
        <div>
          <label>Active Feature: </label>
          <label>{JSON.stringify(this.state.activeMapFeature)}</label>
        </div>
        <Map center={this.state.mapCenter} zoom={4}>
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
          />
          
          {this.state.mapData && this.state.mapData.features.map(mapFeature => (
            <Marker
              key={mapFeature.properties.PARK_ID}
              position={[
                mapFeature.geometry.coordinates[1],
                mapFeature.geometry.coordinates[0]
              ]}
              onClick={() => {
                this.setState({ activeMapFeature: mapFeature});
              }}
              icon={icon}
            />
          ))}
  
          {this.state.activeMapFeature && (
            <Popup
              position={[
                this.state.activeMapFeature.geometry.coordinates[1],
                this.state.activeMapFeature.geometry.coordinates[0]
              ]}
              onClose={() => {
                this.setState({ activeMapFeature: null});
              }}
            >
              <div>
                <h2>{this.state.activeMapFeature.properties.tags.name}</h2>
                <p>{this.state.activeMapFeature.properties.tags.opening_hours}</p>
              </div>
            </Popup>
          )}
        </Map>
      </div>
    );
  }
}

export default App;
